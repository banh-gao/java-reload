package com.github.reload.net;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.apache.log4j.Logger;
import com.github.reload.Components.Component;
import com.github.reload.Components.MessageHandler;
import com.github.reload.Components.start;
import com.github.reload.net.connections.Connection;
import com.github.reload.net.connections.ConnectionManager;
import com.github.reload.net.encoders.Message;
import com.github.reload.net.encoders.content.ContentType;
import com.github.reload.net.encoders.content.errors.ErrorRespose;
import com.github.reload.net.encoders.content.errors.ErrorType;
import com.github.reload.net.encoders.header.Header;
import com.github.reload.net.encoders.header.NodeID;
import com.github.reload.routing.RoutingTable;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

/**
 * Send the outgoing messages to neighbor nodes by using the routing table
 */
public class MessageRouter {

	private final Logger l = Logger.getRootLogger();
	private static final int REQUEST_TIMEOUT = 3000;
	private static final RemovalListener<Long, SettableFuture<Message>> EXP_REQ_LISTERNER = new RemovalListener<Long, SettableFuture<Message>>() {

		@Override
		public void onRemoval(RemovalNotification<Long, SettableFuture<Message>> notification) {
			// Set failure only if request times out
			if (!notification.wasEvicted())
				return;

			SettableFuture<Message> future = notification.getValue();
			future.setException(new RequestTimeoutException());
		}
	};

	@Component
	private ConnectionManager connManager;

	@Component
	private RoutingTable routingTable;

	private Cache<Long, SettableFuture<Message>> pendingRequests;

	@start
	public void compStart() {
		pendingRequests = CacheBuilder.newBuilder().expireAfterWrite(REQUEST_TIMEOUT, TimeUnit.MILLISECONDS).removalListener(EXP_REQ_LISTERNER).build();
	}

	/**
	 * Send the given request message to the destination node into the overlay.
	 * Since the action is performed asyncronously, this method returns
	 * immediately and the returned object can be used to control the delivery
	 * of the message and to receive the answer.
	 * 
	 */
	public ListenableFuture<Message> sendRequestMessage(Message request) {

		final SettableFuture<Message> reqFut = SettableFuture.create();

		pendingRequests.put(request.getHeader().getTransactionId(), reqFut);

		sendMessage(request);

		return reqFut;
	}

	@MessageHandler(value = ContentType.UNKNOWN, handleAnswers = true)
	void handleAnswer(Message message) {
		Long transactionId = message.getHeader().getTransactionId();

		SettableFuture<Message> reqFut = pendingRequests.getIfPresent(transactionId);

		if (reqFut == null) {
			l.debug(String.format("Unattended answer message %#x dropped", message.getHeader().getTransactionId()));
			return;
		}

		pendingRequests.invalidate(transactionId);
		l.debug(String.format("Received answer message %#x", message.getHeader().getTransactionId()));
		reqFut.set(message);
	}

	public void sendMessage(Message message) {
		Header header = message.getHeader();

		Set<NodeID> hops = routingTable.getNextHops(header.getDestinationId());

		for (NodeID nextHop : hops) {
			forward(message, nextHop);
		}
	}

	private void forward(Message message, NodeID nextHop) {
		Connection conn = connManager.getConnection(nextHop);

		conn.write(message);
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

	public static class RequestTimeoutException extends Exception implements ErrorRespose {

		@Override
		public ErrorType getErrorType() {
			return ErrorType.REQUEST_TIMEOUT;
		}
	}
}
