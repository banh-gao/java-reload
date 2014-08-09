package com.github.reload.net;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
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
import com.github.reload.net.encoders.content.Error.ErrorMessageException;
import com.github.reload.net.encoders.content.Error.ErrorType;
import com.github.reload.net.encoders.header.NodeID;
import com.github.reload.routing.RoutingTable;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

/**
 * Send the outgoing messages to neighbor nodes by using the routing table
 */
@Component(MessageRouter.class)
public class MessageRouter {

	private final Logger l = Logger.getRootLogger();
	private static final int REQUEST_TIMEOUT = 5000;

	private final ScheduledExecutorService expiredRequestsRemover = Executors.newScheduledThreadPool(1);

	@Component
	private ConnectionManager connManager;

	@Component
	private MessageBuilder msgBuilder;

	private final Map<Long, SettableFuture<Message>> pendingRequests = Maps.newConcurrentMap();

	@Component
	private ComponentsContext ctx;

	/**
	 * Send the given request message to the destination node into the overlay.
	 * Since the action is performed asyncronously, this method returns
	 * immediately and the returned future will be notified once the answer is
	 * available or the request goes in error
	 * 
	 */
	public ListenableFuture<Message> sendRequestMessage(Message request) {

		final SettableFuture<Message> reqFut = SettableFuture.create();

		long reqId = request.getHeader().getTransactionId();

		pendingRequests.put(reqId, reqFut);

		expiredRequestsRemover.schedule(new ExiredRequestsRemover(reqId), REQUEST_TIMEOUT, TimeUnit.MILLISECONDS);

		sendMessage(request);

		return reqFut;
	}

	public ListenableFuture<NodeID> sendMessage(final Message message) {
		final Header header = message.getHeader();

		final SettableFuture<NodeID> status = SettableFuture.create();

		Set<NodeID> hops;

		if (header.getAttribute(Header.NEXT_HOP) == null) {
			hops = ctx.get(RoutingTable.class).getNextHops(header.getDestinationId());
		} else {
			hops = Collections.singleton(header.getAttribute(Header.NEXT_HOP));
		}

		if (hops.isEmpty()) {
			String err = String.format("No route to %s for message %#x", header.getDestinationId(), header.getTransactionId());
			l.warn(err);
			status.setException(new NetworkException(err));
		}

		for (NodeID nextHop : hops) {
			l.debug(String.format("Sent message %s for %#x to %s through %s", message.getContent().getType(), message.getHeader().getTransactionId(), message.getHeader().getDestinationId(), nextHop));
			transmit(message, nextHop, status);
		}
		return status;
	}

	private void transmit(Message message, final NodeID nextHop, final SettableFuture<NodeID> status) {
		Connection conn = connManager.getConnection(nextHop);

		if (conn == null) {
			status.setException(new NetworkException(String.format("Connection to neighbor node %s not valid", nextHop)));
			return;
		}

		ChannelFuture f = conn.write(message);

		f.addListener(new ChannelFutureListener() {

			@Override
			public void operationComplete(final ChannelFuture future) throws Exception {
				ctx.execute(new Runnable() {

					@Override
					public void run() {
						if (future.isSuccess()) {
							status.set(nextHop);
						} else {
							status.setException(future.cause());
						}
					}

				});

			}
		});
	}

	public void forwardMessage(ForwardMessage msg) {
		Header header = msg.getHeader();
		// Change message header to be forwarded
		header.toForward(header.getAttribute(Header.PREV_HOP));

		// If destination node is directly connected forward message to it
		if (msg.getHeader().getDestinationId() instanceof NodeID) {
			Connection directConn = connManager.getConnection((NodeID) msg.getHeader().getDestinationId());
			if (directConn != null) {
				directConn.forward(msg);
			}
			return;
		}

		for (NodeID nextHop : ctx.get(RoutingTable.class).getNextHops(msg.getHeader().getDestinationId())) {
			Connection c = connManager.getConnection(nextHop);
			// Forward message and ignore delivery status
			c.forward(msg);
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
		Long transactionId = message.getHeader().getTransactionId();

		SettableFuture<Message> reqFut = pendingRequests.get(transactionId);

		if (reqFut == null) {
			l.debug(String.format("Unattended answer message %#x dropped", message.getHeader().getTransactionId()));
			return;
		}

		pendingRequests.remove(transactionId);

		Content content = message.getContent();

		if (content.getType() == ContentType.ERROR) {
			l.debug(String.format("Received error message %s for %#x: %s", ((Error) content).getErrorType(), message.getHeader().getTransactionId(), ((Error) content).toException().getMessage()));
			reqFut.setException(((Error) content).toException());
		} else {
			l.debug(String.format("Received answer message %s for %#x", content.getType(), message.getHeader().getTransactionId()));
			reqFut.set(message);
		}
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

	private class ExiredRequestsRemover implements Runnable {

		private final long transactionId;

		public ExiredRequestsRemover(long transactionId) {
			this.transactionId = transactionId;
		}

		@Override
		public void run() {
			SettableFuture<Message> future = pendingRequests.remove(transactionId);
			if (future != null) {
				future.setException(new RequestTimeoutException(String.format("Request %#x times out", transactionId)));
			}
		}
	}

	public static class RequestTimeoutException extends ErrorMessageException {

		public RequestTimeoutException(String message) {
			super(ErrorType.REQUEST_TIMEOUT, message);
		}
	}
}
