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
import com.github.reload.net.encoders.Message;
import com.github.reload.net.encoders.MessageBuilder;
import com.github.reload.net.encoders.content.Content;
import com.github.reload.net.encoders.content.ContentType;
import com.github.reload.net.encoders.content.Error;
import com.github.reload.net.encoders.content.Error.ErrorMessageException;
import com.github.reload.net.encoders.content.Error.ErrorType;
import com.github.reload.net.encoders.header.Header;
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

	@Component
	private RoutingTable routingTable;

	private Map<Long, SettableFuture<Message>> pendingRequests = Maps.newConcurrentMap();

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

	@MessageHandler(value = ContentType.UNKNOWN, handleAnswers = true)
	void handleAnswer(Message message) {
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

	public ListenableFuture<NodeID> sendMessage(Message message) {
		Header header = message.getHeader();

		SettableFuture<NodeID> status = SettableFuture.create();

		Set<NodeID> hops;

		if (message.getAttribute(Message.NEXT_HOP) == null)
			hops = routingTable.getNextHops(header.getDestinationId());
		else
			hops = Collections.singleton(message.getAttribute(Message.NEXT_HOP));

		for (NodeID nextHop : hops) {
			l.debug(String.format("Sent message %s for %#x to %s through %s", message.getContent().getType(), message.getHeader().getTransactionId(), message.getHeader().getDestinationId(), nextHop));
			forward(message, nextHop, status);
		}
		return status;
	}

	public ListenableFuture<NodeID> sendAnswer(Header requestHdr, Content answer) {
		return sendMessage(msgBuilder.newResponseMessage(requestHdr, answer));
	}

	private void forward(Message message, final NodeID nextHop, final SettableFuture<NodeID> status) {
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
						if (future.isSuccess())
							status.set(nextHop);
						else
							status.setException(future.cause());
					}

				});

			}
		});
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
			super(new Error(ErrorType.REQUEST_TIMEOUT, message));
		}
	}
}
