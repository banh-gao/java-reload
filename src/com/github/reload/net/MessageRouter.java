package com.github.reload.net;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.apache.log4j.Logger;
import com.github.reload.net.ConnectionManager.Connection;
import com.github.reload.net.codecs.ForwardMessage;
import com.github.reload.net.codecs.Header;
import com.github.reload.net.codecs.Message;
import com.github.reload.net.codecs.MessageBuilder;
import com.github.reload.net.codecs.content.Content;
import com.github.reload.net.codecs.content.ContentType;
import com.github.reload.net.codecs.content.Error;
import com.github.reload.net.codecs.content.Error.ErrorMessageException;
import com.github.reload.net.codecs.content.Error.ErrorType;
import com.github.reload.net.codecs.header.NodeID;
import com.github.reload.net.codecs.header.RoutableID;
import com.github.reload.routing.MessageHandlers;
import com.github.reload.routing.MessageHandlers.MessageHandler;
import com.github.reload.routing.TopologyPlugin;
import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import dagger.ObjectGraph;

/**
 * Send the outgoing messages to neighbor nodes by using the routing table
 */
@Singleton
public class MessageRouter {

	private final Logger l = Logger.getRootLogger();

	@Inject
	ConnectionManager connManager;

	@Inject
	MessageBuilder msgBuilder;

	@Inject
	TopologyPlugin topology;

	@Inject
	@Named("packetsLooper")
	Executor exec;

	private final RequestManager reqManager = new RequestManager();

	@Inject
	public MessageRouter(MessageHandlers msgHandlers) {
		msgHandlers.register(this);
	}

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
			return topology.getRoutingTable().getNextHops(dest);
	}

	private boolean isDirectlyConnected(NodeID nextDest) {
		return connManager.isNeighbor(nextDest);
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
				exec.execute(new Runnable() {

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
						if (future.isSuccess()) {
							l.debug(String.format("Message %#x forwarded to %s", msg.getHeader().getTransactionId(), directConn.get().getNodeId()), future.cause());
						} else {
							l.debug(String.format("Message forwarding of %#x failed", msg.getHeader().getTransactionId()), future.cause());
						}
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
					if (future.isSuccess()) {
						l.debug(String.format("Message %#x forwarded to %s", msg.getHeader().getTransactionId(), c.get().getNodeId()), future.cause());
					} else {
						l.debug(String.format("Message forwarding of %#x failed", msg.getHeader().getTransactionId()), future.cause());
					}
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
		reqManager.handleAnswer(message, null);
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

	private class RequestManager {

		private static final int REQUEST_TIMEOUT = 5000;

		private final Logger l = Logger.getRootLogger();

		private final ScheduledExecutorService expiredRequestsRemover = Executors.newScheduledThreadPool(1);

		private final Map<Long, PendingRequest> pendingRequests = Maps.newConcurrentMap();

		public SettableFuture<Message> put(Message request) {

			Long reqId = request.getHeader().getTransactionId();

			SettableFuture<Message> future = SettableFuture.create();

			PendingRequest p = new PendingRequest(request, future);

			pendingRequests.put(reqId, p);

			expiredRequestsRemover.schedule(new ExiredRequestsRemover(reqId), REQUEST_TIMEOUT, TimeUnit.MILLISECONDS);

			return future;
		}

		public void handleAnswer(Message message, ObjectGraph ctx) {
			Long transactionId = message.getHeader().getTransactionId();

			Optional<PendingRequest> pending = Optional.fromNullable(pendingRequests.remove(transactionId));

			if (!pending.isPresent()) {
				l.debug(String.format("Unattended answer message %#x dropped", message.getHeader().getTransactionId()));
				return;
			}

			Content content = message.getContent();

			if (content.getType() == ContentType.ERROR) {
				l.debug(String.format("Received error message %s for %#x: %s", ((Error) content).getErrorType(), message.getHeader().getTransactionId(), ((Error) content).toException().getMessage()));
				pending.get().setException(((Error) content).toException());
			} else {
				l.debug(String.format("Received answer message %s for %#x", content.getType(), message.getHeader().getTransactionId()));
				pending.get().setAnswer(message, ctx);
			}
		}

		private class PendingRequest {

			private final RoutableID reqDest;
			private final SettableFuture<Message> future;

			public PendingRequest(Message req, SettableFuture<Message> future) {
				reqDest = req.getHeader().getDestinationId();
				this.future = future;
			}

			public void setAnswer(Message ans, ObjectGraph ctx) {
				try {
					validateAnswer(ans, ctx);
					future.set(ans);
				} catch (GeneralSecurityException e) {
					future.setException(e);
				}

			}

			private void validateAnswer(Message ans, ObjectGraph ctx) throws GeneralSecurityException {

				NodeID sender = ans.getHeader().getSenderId();

				switch (reqDest.getType()) {
					case NODEID :
						if (!reqDest.equals(sender))
							throw new GeneralSecurityException("Answering node not matching with request destination node");
						break;

					case RESOURCEID :
						Set<NodeID> neighbors = new HashSet<NodeID>(topology.getRoutingTable().getNeighbors());
						neighbors.add(sender);

						// Sender node should be closer than any of the neighbor
						// nodes to the destination resource
						if (!topology.getCloserId(reqDest, neighbors).equals(sender))
							throw new GeneralSecurityException("Answering node is not closer than neighbors to the request destination resource");

						break;

					default :
						throw new IllegalStateException();
				}
			}

			public void setException(Throwable cause) {
				future.setException(cause);
			}
		}

		private class ExiredRequestsRemover implements Runnable {

			private final long transactionId;

			public ExiredRequestsRemover(long transactionId) {
				this.transactionId = transactionId;
			}

			@Override
			public void run() {
				PendingRequest pending = pendingRequests.remove(transactionId);

				if (pending == null)
					return;

				pending.setException(new RequestTimeoutException(String.format("Request %#x times out", transactionId)));
			}
		}

		public class RequestTimeoutException extends ErrorMessageException {

			public RequestTimeoutException(String message) {
				super(ErrorType.REQUEST_TIMEOUT, message);
			}
		}
	}
}
