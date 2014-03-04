package com.github.reload.net.connections;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.nio.NioEventLoopGroup;
import java.util.Map;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import com.github.reload.Context;
import com.github.reload.message.ContentType;
import com.github.reload.message.DestinationList;
import com.github.reload.message.NodeID;
import com.github.reload.message.RoutableID;
import com.github.reload.message.content.AttachMessage;
import com.github.reload.message.errors.NetworkException;
import com.github.reload.net.MessageTransmitter;
import com.github.reload.net.data.Message;
import com.github.reload.net.ice.ICEHelper;
import com.github.reload.net.ice.IceCandidate;
import com.github.reload.net.ice.NoSuitableCandidateException;
import com.google.common.collect.Maps;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

/**
 * Establish and manage connections for all neighbor nodes
 */
public class ConnectionManager {

	private static final Logger l = Logger.getRootLogger();

	private final Map<NodeID, Connection> connections = Maps.newHashMap();

	private final Map<NodeID, SettableFuture<Connection>> pendingConnections = Maps.newHashMap();

	private final ICEHelper iceHelper;
	private final MessageTransmitter msgTransmitter;

	private NioEventLoopGroup clientLoopGroup;

	public ConnectionManager(Context context) {
		this.iceHelper = context.getIceHelper();
		this.msgTransmitter = context.getMessageTransmitter();
		clientLoopGroup = new NioEventLoopGroup();
	}

	public ListenableFuture<Connection> connect(DestinationList destList, boolean requestUpdate) {
		// TODO: create connector to manages new connections through attach
		// messages
		SettableFuture<Connection> fut = SettableFuture.create();

		RoutableID destinationID = destList.getDestination();

		if (destinationID instanceof NodeID) {
			Connection c = connections.get(destinationID);
			if (c != null) {
				fut.set(c);
				return fut;
			}
		}

		AttachMessage.Builder b = new AttachMessage.Builder();
		b.candidates(iceHelper.getCandidates(attachServer.getLocalSocketAddress()));
		b.sendUpdate(requestUpdate);
		AttachMessage attachRequest = b.buildRequest();

		Message req = context.getMessageBuilder().newMessage(attachRequest, destList);

		l.log(Level.DEBUG, "Attach to " + destinationID + " in progress...");

		ListenableFuture<Message> ansFut = msgTransmitter.sendRequestMessage(req);
	}

	@Subscribe
	public void attachAnswerReceived(Message msg) {
		if (msg.getContent().getType() != ContentType.ATTACH_ANS)
			return;

		AttachMessage answer = (AttachMessage) msg.getContent();

		final NodeID remoteNode = msg.getHeader().getSenderId();

		final IceCandidate remoteCandidate;

		try {
			remoteCandidate = iceHelper.testAndSelectCandidate(answer.getCandidates());
		} catch (NoSuitableCandidateException e1) {
			throw new NetworkException("No suitable direct connection parameters found");
		}

		ChannelFuture chFut = ClientChannelFactory.connectTo(remoteCandidate.getSocketAddress(), remoteCandidate.getOverlayLinkType(), clientLoopGroup, context);

		chFut.addListener(new ChannelFutureListener() {

			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				Connection newConnection = new Connection(remoteCandidate.getOverlayLinkType(), future.channel());
				SettableFuture<Connection> connFut = pendingConnections.get(remoteNode);
				if (connFut == null)
					return;

				connFut.set(newConnection);
				l.log(Level.DEBUG, "Attach to " + remoteNode + " at " + future.channel().remoteAddress() + " completed");
			}
		});
	}
}
