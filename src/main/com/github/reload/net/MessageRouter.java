package com.github.reload.net;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import org.apache.log4j.Logger;
import com.github.reload.components.ComponentsContext;
import com.github.reload.components.ComponentsRepository.Component;
import com.github.reload.components.MessageHandlersManager.MessageHandler;
import com.github.reload.net.connections.Connection;
import com.github.reload.net.connections.ConnectionManager;
import com.github.reload.net.encoders.ForwardMessage;
import com.github.reload.net.encoders.Header;
import com.github.reload.net.encoders.Message;
import com.github.reload.net.encoders.MessageBuilder;
import com.github.reload.net.encoders.content.Content;
import com.github.reload.net.encoders.content.ContentType;
import com.github.reload.net.encoders.content.Error;
import com.github.reload.net.encoders.content.Error.ErrorType;
import com.github.reload.net.encoders.header.NodeID;
import com.github.reload.net.encoders.header.RoutableID;
import com.github.reload.routing.RoutingTable;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

/**
 * Send the outgoing messages to neighbor nodes by using the routing table
 */
@Component(value = MessageRouter.class, priority = 0)
public class MessageRouter {

	private final Logger l = Logger.getRootLogger();

	@Component
	private ConnectionManager connManager;

	@Component
	private MessageBuilder msgBuilder;

	@Component
	private ComponentsContext ctx;

	private RequestManager reqManager = new RequestManager();

	/**
	 * Send the given request message to the destination node into the overlay.
	 * Since the action is performed asyncronously, this method returns
	 * immediately and the returned future will be notified once the answer is
	 * available or the request goes in error
	 * 
	 */
	public ListenableFuture<Message> sendRequestMessage(final Message request) {
		final SettableFuture<Message> reqFut = reqManager.put(request);

		ListenableFuture<NodeID> linkStatus = sendMessage(request);
		Futures.addCallback(linkStatus, new FutureCallback<NodeID>() {

			@Override
			public void onSuccess(NodeID result) {
			}

			@Override
			public void onFailure(Throwable t) {
				// Fail fast request when neighbor transmission fails
				reqFut.setException(t);
			}
		});

		return reqFut;
	}

	public ListenableFuture<NodeID> sendMessage(final Message message) {
		final Header header = message.getHeader();

		final SettableFuture<NodeID> status = SettableFuture.create();

		RoutableID nextDest = header.getNextHop();

		Set<NodeID> hops = getNextHops(nextDest);

		if (hops.isEmpty()) {
			String err = String.format("No route to %s for message %#x", header.getNextHop(), header.getTransactionId());
			l.warn(err);
			status.setException(new NetworkException(err));
			return status;
		}

		for (NodeID nextHop : hops) {
			transmit(message, nextHop, status);
		}

		return status;
	}

	private Set<NodeID> getNextHops(RoutableID dest) {
		if (dest instanceof NodeID && isDirectlyConnected((NodeID) dest))
			return Collections.singleton((NodeID) dest);
		else
			return ctx.get(RoutingTable.class).getNextHops(dest);
	}

	private boolean isDirectlyConnected(NodeID nextDest) {
		return ctx.get(ConnectionManager.class).isNeighbor(nextDest);
	}

	private void transmit(final Message message, final NodeID neighborNode, final SettableFuture<NodeID> status) {
		final Optional<Connection> conn = connManager.getConnection(neighborNode);

		if (!conn.isPresent()) {
			status.setException(new NetworkException(String.format("Connection to neighbor node %s not valid", neighborNode)));
			return;
		}

		ChannelFuture f = conn.get().write(message);

		f.addListener(new ChannelFutureListener() {

			@Override
			public void operationComplete(final ChannelFuture future) throws Exception {
				l.debug(String.format("Transmitting message %#x (%s) to %s through %s at %s...", message.getHeader().getTransactionId(), message.getContent().getType(), message.getHeader().getDestinationId(), neighborNode, conn.get().getStack().getChannel().remoteAddress()));
				ctx.execute(new Runnable() {

					@Override
					public void run() {
						if (future.isSuccess()) {
							l.debug(String.format("Message %#x transmitted to %s through %s", message.getHeader().getTransactionId(), message.getHeader().getDestinationId(), neighborNode));
							status.set(neighborNode);
						} else {
							l.debug(String.format("Message %#x transmission to %s through %s failed", message.getHeader().getTransactionId(), message.getHeader().getDestinationId(), neighborNode), future.cause());
							status.setException(future.cause());
						}
					}

				});

			}
		});
	}

	public void forwardMessage(final ForwardMessage msg) {
		Header header = msg.getHeader();
		// Change message header to be forwarded
		header.toForward(header.getAttribute(Header.PREV_HOP));

		// If destination node is directly connected forward message to it
		if (msg.getHeader().getNextHop() instanceof NodeID) {
			final Optional<Connection> directConn = connManager.getConnection((NodeID) msg.getHeader().getNextHop());
			if (directConn.isPresent()) {
				ChannelFuture fut = directConn.get().forward(msg);
				fut.addListener(new ChannelFutureListener() {

					@Override
					public void operationComplete(ChannelFuture future) throws Exception {
						if (future.isSuccess())
							l.debug(String.format("Message %#x forwarded to %s", msg.getHeader().getTransactionId(), directConn.get().getNodeId()), future.cause());
						else
							l.debug(String.format("Message forwarding of %#x failed", msg.getHeader().getTransactionId()), future.cause());
					}
				});
			}
			return;
		}

		for (NodeID nextHop : getNextHops(msg.getHeader().getNextHop())) {
			final Optional<Connection> c = connManager.getConnection(nextHop);

			// Forward message and ignore delivery status
			ChannelFuture fut = c.get().forward(msg);
			fut.addListener(new ChannelFutureListener() {

				@Override
				public void operationComplete(ChannelFuture future) throws Exception {
					if (future.isSuccess())
						l.debug(String.format("Message %#x forwarded to %s", msg.getHeader().getTransactionId(), c.get().getNodeId()), future.cause());
					else
						l.debug(String.format("Message forwarding of %#x failed", msg.getHeader().getTransactionId()), future.cause());
				}
			});
		}
	}

	public ListenableFuture<NodeID> sendAnswer(Header requestHdr, Content answer) {
		return sendMessage(msgBuilder.newResponseMessage(requestHdr, answer));
	}

	public ListenableFuture<NodeID> sendError(Header requestHdr, ErrorType type, byte[] info) {
		return sendAnswer(requestHdr, new Error(type, info));
	}

	public ListenableFuture<NodeID> sendError(Header requestHdr, ErrorType type, String info) {
		return sendAnswer(requestHdr, new Error(type, info));
	}

	@MessageHandler(handleAnswers = true, value = ContentType.ERROR)
	private void handleAnswer(Message message) {
		reqManager.handleAnswer(message, ctx);
	}

	public static class ForwardingException extends Exception {

		private final Map<NodeID, Throwable> failures;

		public ForwardingException(Map<NodeID, Throwable> failures) {
			this.failures = failures;
		}

		public Map<NodeID, Throwable> getFailures() {
			return failures;
		}

	}
}
