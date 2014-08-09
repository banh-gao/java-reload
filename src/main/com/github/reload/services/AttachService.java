package com.github.reload.services;

import java.util.Map;
import java.util.Set;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import com.github.reload.Bootstrap;
import com.github.reload.components.ComponentsContext;
import com.github.reload.components.ComponentsRepository.Component;
import com.github.reload.components.MessageHandlersManager.MessageHandler;
import com.github.reload.net.MessageRouter;
import com.github.reload.net.connections.Connection;
import com.github.reload.net.connections.ConnectionManager;
import com.github.reload.net.connections.ConnectionManager.ConnectionStatusEvent;
import com.github.reload.net.connections.ConnectionManager.ConnectionStatusEvent.Type;
import com.github.reload.net.encoders.Header;
import com.github.reload.net.encoders.Message;
import com.github.reload.net.encoders.MessageBuilder;
import com.github.reload.net.encoders.content.AttachMessage;
import com.github.reload.net.encoders.content.ContentType;
import com.github.reload.net.encoders.content.Error.ErrorType;
import com.github.reload.net.encoders.header.DestinationList;
import com.github.reload.net.encoders.header.NodeID;
import com.github.reload.net.encoders.header.RoutableID;
import com.github.reload.net.ice.HostCandidate;
import com.github.reload.net.ice.ICEHelper;
import com.github.reload.net.ice.NoSuitableCandidateException;
import com.github.reload.routing.TopologyPlugin;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

/**
 * Establish direct connections to other peers using attach messages
 */
@Component(AttachService.class)
public class AttachService {

	private static final Logger l = Logger.getRootLogger();

	@Component
	private Bootstrap bootstrap;

	@Component
	private MessageBuilder msgBuilder;

	@Component
	private MessageRouter msgRouter;

	@Component
	ConnectionManager connMgr;

	@Component
	private ICEHelper iceHelper;

	@Component
	private TopologyPlugin plugin;

	@Component
	private ComponentsContext ctx;

	private final Map<Long, SettableFuture<Connection>> pendingRequests = Maps.newConcurrentMap();
	private final Set<NodeID> answeredRequests = Sets.newConcurrentHashSet();

	private final Set<NodeID> updateAfterConnection = Sets.newConcurrentHashSet();

	public ListenableFuture<Connection> attachTo(DestinationList destList, NodeID nextHop, boolean requestUpdate) {
		final SettableFuture<Connection> fut = SettableFuture.create();
		final RoutableID destinationID = destList.getDestination();

		if (destinationID instanceof NodeID) {
			Connection c = connMgr.getConnection((NodeID) destinationID);
			if (c != null) {
				l.info(String.format("Attach to %s not performed, already connected", c.getNodeId()));
				fut.set(c);
				return fut;
			}
		}

		AttachMessage.Builder b = new AttachMessage.Builder();
		b.candidates(iceHelper.getCandidates(connMgr.getServerAddress()));
		b.sendUpdate(requestUpdate);
		AttachMessage attachRequest = b.buildRequest();

		final Message req = msgBuilder.newMessage(attachRequest, destList);

		Header reqHeader = req.getHeader();

		if (nextHop != null) {
			reqHeader.setAttribute(Header.NEXT_HOP, nextHop);
		}

		pendingRequests.put(reqHeader.getTransactionId(), fut);

		l.log(Level.DEBUG, "Attach to " + destinationID + " in progress...");

		ListenableFuture<Message> attachAnsFut = msgRouter.sendRequestMessage(req);

		Futures.addCallback(attachAnsFut, new FutureCallback<Message>() {

			@Override
			public void onSuccess(Message result) {
				processAttachAnswer(result);
			}

			@Override
			public void onFailure(Throwable t) {
				pendingRequests.remove(req.getHeader().getTransactionId());
				answeredRequests.remove(destinationID);

				l.info(String.format("Attach to %s failed", destinationID));
				fut.setException(t);
			}
		});

		return fut;
	}

	private void processAttachAnswer(Message msg) {

		AttachMessage answer = (AttachMessage) msg.getContent();

		final NodeID remoteNode = msg.getHeader().getSenderId();

		final long transactionId = msg.getHeader().getTransactionId();

		if (!pendingRequests.containsKey(transactionId)) {
			l.log(Level.DEBUG, String.format("Unattended attach answer %#x ignored", msg.getHeader().getTransactionId()));
			return;
		}

		answeredRequests.add(remoteNode);

		final HostCandidate remoteCandidate;

		try {
			remoteCandidate = iceHelper.testAndSelectCandidate(answer.getCandidates());

			l.debug(String.format("Attach negotiation with " + remoteNode + " completed, selected remote candidate: %s...", remoteCandidate.getSocketAddress()));

			ListenableFuture<Connection> connFut = connMgr.connectTo(remoteCandidate.getSocketAddress(), remoteCandidate.getOverlayLinkType());

			Futures.addCallback(connFut, new FutureCallback<Connection>() {

				@Override
				public void onSuccess(Connection result) {
					SettableFuture<Connection> pendingAttach = pendingRequests.remove(transactionId);
					answeredRequests.remove(remoteNode);
					if (pendingAttach != null) {
						pendingAttach.set(result);
					}
				}

				@Override
				public void onFailure(Throwable t) {
					SettableFuture<Connection> pendingAttach = pendingRequests.remove(transactionId);
					answeredRequests.remove(remoteNode);
					if (pendingAttach != null) {
						pendingAttach.setException(t);
					}
				}
			});

		} catch (NoSuitableCandidateException e) {
			l.info(e.getMessage());
		}

	}

	@MessageHandler(ContentType.ATTACH_REQ)
	public void handleAttachRequest(Message req) {
		NodeID sender = req.getHeader().getSenderId();

		// No pending request, just send the answer
		if (!pendingRequests.containsKey(req.getHeader().getTransactionId())) {
			l.debug("Attach request from " + sender + " received, sending local candidates list...");
			sendAnswer(req);
			return;
		}

		// Pending request already answered
		if (answeredRequests.contains(sender)) {
			msgRouter.sendError(req.getHeader(), ErrorType.IN_PROGRESS, "Attach already in progress");
			return;
		}

		// Pending request not answered
		if (bootstrap.getLocalNodeId().compareTo(sender) > 0) {
			msgRouter.sendError(req.getHeader(), ErrorType.IN_PROGRESS, "Attach request already sent");
			return;
		}

		sendAnswer(req);
	}

	private void sendAnswer(Message req) {
		AttachMessage.Builder b = new AttachMessage.Builder();
		b.candidates(iceHelper.getCandidates(connMgr.getServerAddress()));
		AttachMessage attachAnswer = b.buildAnswer();

		Header reqHeader = req.getHeader();

		// Send attach answer through the same neighbor
		reqHeader.setAttribute(Header.NEXT_HOP, reqHeader.getAttribute(Header.PREV_HOP));

		msgRouter.sendAnswer(reqHeader, attachAnswer);

		AttachMessage attachRequest = (AttachMessage) req.getContent();

		if (attachRequest.isSendUpdate()) {
			updateAfterConnection.add(req.getHeader().getSenderId());
		}
	}

	@Subscribe
	public void sendUpdateAfterConnection(ConnectionStatusEvent e) {
		if (e.type == Type.ACCEPTED && updateAfterConnection.remove(e.connection.getNodeId())) {
			plugin.requestUpdate(e.connection.getNodeId());
		}
	}
}
