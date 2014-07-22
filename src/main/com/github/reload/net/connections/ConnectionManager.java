package com.github.reload.net.connections;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import java.net.InetSocketAddress;
import java.util.Map;
import javax.net.ssl.SSLEngine;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import com.github.reload.Components.Component;
import com.github.reload.Components.stop;
import com.github.reload.Configuration;
import com.github.reload.MessageBuilder;
import com.github.reload.crypto.CryptoHelper;
import com.github.reload.net.MessageRouter;
import com.github.reload.net.encoders.Message;
import com.github.reload.net.encoders.content.AttachMessage;
import com.github.reload.net.encoders.header.DestinationList;
import com.github.reload.net.encoders.header.NodeID;
import com.github.reload.net.encoders.header.RoutableID;
import com.github.reload.net.ice.ICEHelper;
import com.github.reload.net.ice.IceCandidate;
import com.github.reload.net.ice.IceCandidate.OverlayLinkType;
import com.github.reload.net.ice.NoSuitableCandidateException;
import com.github.reload.net.server.ServerManager;
import com.github.reload.net.stack.MessageDispatcher;
import com.github.reload.net.stack.ReloadStack;
import com.github.reload.net.stack.ReloadStackBuilder;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

/**
 * Establish and manage connections for all neighbor nodes
 */
public class ConnectionManager {

	private static final Logger l = Logger.getRootLogger();

	private final Map<NodeID, Connection> connections = Maps.newHashMap();

	private final Map<RoutableID, SettableFuture<Connection>> pendingConnections = Maps.newHashMap();

	private final MessageDispatcher msgDispatcher = new MessageDispatcher();

	@Component
	private ICEHelper iceHelper;

	@Component
	private MessageRouter msgRouter;

	@Component
	private MessageBuilder msgBuilder;

	@Component
	private ServerManager serverManager;

	@Component
	private CryptoHelper cryptoHelper;

	@Component
	private Configuration conf;

	@stop
	private void shutdown() {
		for (Connection c : connections.values())
			c.close();
	}

	public ListenableFuture<Connection> attachTo(DestinationList destList, boolean requestUpdate) {
		final SettableFuture<Connection> fut = SettableFuture.create();
		final RoutableID destinationID = destList.getDestination();

		if (destinationID instanceof NodeID) {
			Connection c = connections.get(destinationID);
			if (c != null) {
				fut.set(c);
				return fut;
			}
		}

		AttachMessage.Builder b = new AttachMessage.Builder();
		b.candidates(iceHelper.getCandidates(serverManager.getAttachServer().getLocalSocketAddress()));
		b.sendUpdate(requestUpdate);
		AttachMessage attachRequest = b.buildRequest();

		Message req = msgBuilder.newMessage(attachRequest, destList);

		l.log(Level.DEBUG, "Attach to " + destinationID + " in progress...");

		pendingConnections.put(destinationID, fut);

		ListenableFuture<Message> attachAnsFut = msgRouter.sendRequestMessage(req);

		Futures.addCallback(attachAnsFut, new FutureCallback<Message>() {

			@Override
			public void onSuccess(Message result) {
				attachAnswerReceived(result);
			}

			@Override
			public void onFailure(Throwable t) {
				pendingConnections.remove(destinationID);

				fut.setException(t);
			}
		});

		return fut;
	}

	private void attachAnswerReceived(Message msg) {

		AttachMessage answer = (AttachMessage) msg.getContent();

		final NodeID remoteNode = msg.getHeader().getSenderId();

		if (!pendingConnections.containsKey(remoteNode)) {
			l.log(Level.DEBUG, String.format("Unattended attach answer %#x ignored", msg.getHeader().getTransactionId()));
			return;
		}

		final IceCandidate remoteCandidate;

		try {
			remoteCandidate = iceHelper.testAndSelectCandidate(answer.getCandidates());
			connectTo(remoteNode, remoteCandidate.getSocketAddress(), remoteCandidate.getOverlayLinkType());
		} catch (NoSuitableCandidateException e) {
			// TODO: send error message
			e.printStackTrace();
		}

	}

	public ListenableFuture<Connection> connectTo(final NodeID remoteId, final InetSocketAddress remoteAddr, OverlayLinkType linkType) {
		final ReloadStack stack;

		final SettableFuture<Connection> outcome = SettableFuture.create();

		try {
			ReloadStackBuilder b = new ReloadStackBuilder(conf, msgDispatcher);
			b.setLinkType(linkType);
			SSLEngine sslEngine = cryptoHelper.getClientSSLEngine(linkType);
			if (sslEngine != null)
				b.setSslEngine(sslEngine);
			stack = b.buildStack();
		} catch (Exception e) {
			outcome.setException(e);
			return outcome;
		}
		ChannelFuture cf = stack.connect(remoteAddr);

		cf.addListener(new ChannelFutureListener() {

			@Override
			public void operationComplete(ChannelFuture future) throws Exception {

				final SettableFuture<Connection> pendingAttach = pendingConnections.remove(remoteId);

				if (future.isSuccess()) {
					Connection c = new Connection(remoteId, stack);

					connections.put(remoteId, c);
					l.debug("Connection to " + remoteId + " at " + remoteAddr + " completed");
					outcome.set(c);

					if (pendingAttach != null) {
						pendingAttach.set(c);
					}
				} else {
					l.warn("Connection to " + remoteId + " at " + remoteAddr + " failed", future.cause());
					outcome.setException(future.cause());
					if (pendingAttach != null) {
						pendingAttach.setException(future.cause());
					}
				}

			}
		});

		return outcome;
	}

	public Connection getConnection(NodeID neighbor) {
		return connections.get(neighbor);
	}
}
