package com.github.reload.net.connections;

import java.util.Map;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import com.github.reload.Components.Component;
import com.github.reload.Components.MessageHandler;
import com.github.reload.net.MessageRouter;
import com.github.reload.net.encoders.Message;
import com.github.reload.net.encoders.MessageBuilder;
import com.github.reload.net.encoders.content.AttachMessage;
import com.github.reload.net.encoders.content.ContentType;
import com.github.reload.net.encoders.header.DestinationList;
import com.github.reload.net.encoders.header.NodeID;
import com.github.reload.net.encoders.header.RoutableID;
import com.github.reload.net.ice.ICEHelper;
import com.github.reload.net.ice.IceCandidate;
import com.github.reload.net.ice.NoSuitableCandidateException;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

/**
 * Establish direct connections to other peers using attach messages
 */
@Component(AttachConnector.COMPNAME)
public class AttachConnector {

	public static final String COMPNAME = "com.github.reload.net.connections.AttachConnector";

	private static final Logger l = Logger.getRootLogger();

	@Component
	private MessageBuilder msgBuilder;

	@Component
	private MessageRouter msgRouter;

	@Component
	ConnectionManager connMgr;

	@Component
	private ICEHelper iceHelper;

	private final Map<RoutableID, SettableFuture<Connection>> pendingConnections = Maps.newHashMap();

	public ListenableFuture<Connection> attachTo(DestinationList destList, NodeID nextHop, boolean requestUpdate) {
		final SettableFuture<Connection> fut = SettableFuture.create();
		final RoutableID destinationID = destList.getDestination();

		if (destinationID instanceof NodeID) {
			Connection c = connMgr.getConnection((NodeID) destinationID);
			if (c != null) {
				fut.set(c);
				return fut;
			}
		}

		AttachMessage.Builder b = new AttachMessage.Builder();
		b.candidates(iceHelper.getCandidates(connMgr.getServerAddress()));
		b.sendUpdate(requestUpdate);
		AttachMessage attachRequest = b.buildRequest();

		Message req = msgBuilder.newMessage(attachRequest, destList);

		l.log(Level.DEBUG, "Attach to " + destinationID + " in progress...");

		pendingConnections.put(destinationID, fut);

		if (nextHop != null)
			req.setAttribute(Message.NEXT_HOP, nextHop);

		ListenableFuture<Message> attachAnsFut = msgRouter.sendRequestMessage(req);

		Futures.addCallback(attachAnsFut, new FutureCallback<Message>() {

			@Override
			public void onSuccess(Message result) {
				processAttachAnswer(result);
			}

			@Override
			public void onFailure(Throwable t) {
				pendingConnections.remove(destinationID);

				fut.setException(t);
			}
		});

		return fut;
	}

	private void processAttachAnswer(Message msg) {

		AttachMessage answer = (AttachMessage) msg.getContent();

		final NodeID remoteNode = msg.getHeader().getSenderId();

		if (!pendingConnections.containsKey(remoteNode)) {
			l.log(Level.DEBUG, String.format("Unattended attach answer %#x ignored", msg.getHeader().getTransactionId()));
			return;
		}

		final IceCandidate remoteCandidate;

		try {
			remoteCandidate = iceHelper.testAndSelectCandidate(answer.getCandidates());
			ListenableFuture<Connection> connFut = connMgr.connectTo(remoteCandidate.getSocketAddress(), remoteCandidate.getOverlayLinkType());

			Futures.addCallback(connFut, new FutureCallback<Connection>() {

				@Override
				public void onSuccess(Connection result) {
					SettableFuture<Connection> pendingAttach = pendingConnections.remove(remoteNode);

					if (pendingAttach != null) {
						pendingAttach.set(result);
					}
				}

				@Override
				public void onFailure(Throwable t) {
					SettableFuture<Connection> pendingAttach = pendingConnections.remove(remoteNode);
					if (pendingAttach != null) {
						pendingAttach.setException(t);
					}
				}
			});

		} catch (NoSuitableCandidateException e) {
			// TODO: send error message
			e.printStackTrace();
		}

	}

	@MessageHandler(ContentType.ATTACH_REQ)
	public void handleAttachRequest(Message req) {
		// TODO
	}

	@MessageHandler(ContentType.LEAVE_REQ)
	public void handleLeaveRequest(Message req) {
		// TODO
	}
}
