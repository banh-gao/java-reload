package com.github.reload.net;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import com.github.reload.message.ContentType;
import com.github.reload.message.errors.ErrorRespose;
import com.github.reload.message.errors.ErrorType;
import com.github.reload.net.MessageReceiver.MessageProcessor;
import com.github.reload.net.MessageRouter.ForwardPromise;
import com.github.reload.net.data.Message;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

/**
 * Transmit messages on the overlay and provide the request-answer model
 * management
 */
public class MessageTransmitter implements MessageProcessor {

	private static final Set<ContentType> HANDLED_CONTENTS = new HashSet<ContentType>();

	static {
		// Handle all answer messages
		for (ContentType t : ContentType.values())
			if (t.isAnswer())
				HANDLED_CONTENTS.add(t);
	}

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

	private final Logger l = Logger.getRootLogger();

	private final MessageRouter router;
	private final Cache<Long, SettableFuture<Message>> pendingRequests;

	public MessageTransmitter(MessageRouter router) {
		this.router = router;
		pendingRequests = CacheBuilder.newBuilder().expireAfterWrite(REQUEST_TIMEOUT, TimeUnit.MILLISECONDS).removalListener(EXP_REQ_LISTERNER).build();
	}

	/**
	 * Send the given message to the destination node in the overlay. Since the
	 * transmission is performed asyncronously, this method returns immediately
	 * and the returned {@link RequestPromise} can be used to control the
	 * delivery of the message.
	 * 
	 * @param message
	 * @return
	 */
	public ListenableFuture<Message> sendRequestMessage(Message request) {
		ForwardPromise fwdPrm = router.sendMessage(request);

		final SettableFuture<Message> reqFut = SettableFuture.create();

		// Fail fast if request fails already in the neighbors transmission
		fwdPrm.addListener(new MessageRouter.ForwardPromiseListener() {

			@Override
			public void operationComplete(ForwardPromise future) throws Exception {
				// Fails only if the forward fails to ALL the neighbor nodes
				if (!future.isSuccess() && future.getSuccessNeighbors().size() == 0) {
					reqFut.setException(future.cause());
				}
			}
		});

		pendingRequests.put(request.getHeader().getTransactionId(), reqFut);

		return reqFut;
	}

	/**
	 * Send a message without waiting for an answer
	 * 
	 * @param message
	 * @return
	 */
	public ForwardPromise sendMessage(Message message) {
		return router.sendMessage(message);
	}

	@Override
	public Set<ContentType> getAcceptedTypes() {
		return HANDLED_CONTENTS;
	}

	@Override
	public void processMessage(Message message) {
		Long transactionId = message.getHeader().getTransactionId();

		SettableFuture<Message> reqFut = pendingRequests.getIfPresent(transactionId);

		if (reqFut == null) {
			l.log(Level.DEBUG, "Unexpected answer dropped: " + message);
			return;
		}

		pendingRequests.invalidate(transactionId);

		reqFut.set(message);
	}

	public static class RequestTimeoutException extends Exception implements ErrorRespose {

		@Override
		public ErrorType getErrorType() {
			return ErrorType.REQUEST_TIMEOUT;
		}
	}
}
