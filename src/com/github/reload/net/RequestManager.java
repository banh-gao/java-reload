package com.github.reload.net;

import java.security.GeneralSecurityException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import org.apache.log4j.Logger;
import com.github.reload.components.ComponentsContext;
import com.github.reload.net.encoders.Message;
import com.github.reload.net.encoders.content.Content;
import com.github.reload.net.encoders.content.ContentType;
import com.github.reload.net.encoders.content.Error;
import com.github.reload.net.encoders.content.Error.ErrorMessageException;
import com.github.reload.net.encoders.content.Error.ErrorType;
import com.github.reload.net.encoders.header.NodeID;
import com.github.reload.net.encoders.header.RoutableID;
import com.github.reload.routing.RoutingTable;
import com.github.reload.routing.TopologyPlugin;
import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.SettableFuture;

class RequestManager {

	private static final int REQUEST_TIMEOUT = 5000;

	private final Logger l = Logger.getRootLogger();

	private final ScheduledExecutorService expiredRequestsRemover = Executors.newScheduledThreadPool(1);

	private final Map<Long, PendingRequest> pendingRequests = Maps.newConcurrentMap();

	@Inject
	RoutingTable routingTable;

	@Inject
	TopologyPlugin topology;

	public SettableFuture<Message> put(Message request) {

		Long reqId = request.getHeader().getTransactionId();

		SettableFuture<Message> future = SettableFuture.create();

		PendingRequest p = new PendingRequest(request, future);

		pendingRequests.put(reqId, p);

		expiredRequestsRemover.schedule(new ExiredRequestsRemover(reqId), REQUEST_TIMEOUT, TimeUnit.MILLISECONDS);

		return future;
	}

	public void handleAnswer(Message message, ComponentsContext ctx) {
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

		public void setAnswer(Message ans, ComponentsContext ctx) {
			try {
				validateAnswer(ans, ctx);
				future.set(ans);
			} catch (GeneralSecurityException e) {
				future.setException(e);
			}

		}

		private void validateAnswer(Message ans, ComponentsContext ctx) throws GeneralSecurityException {

			NodeID sender = ans.getHeader().getSenderId();

			switch (reqDest.getType()) {
				case NODEID :
					if (!reqDest.equals(sender))
						throw new GeneralSecurityException("Answering node not matching with request destination node");
					break;

				case RESOURCEID :
					Set<NodeID> neighbors = new HashSet<NodeID>(routingTable.getNeighbors());
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

	public static class RequestTimeoutException extends ErrorMessageException {

		public RequestTimeoutException(String message) {
			super(ErrorType.REQUEST_TIMEOUT, message);
		}
	}
}