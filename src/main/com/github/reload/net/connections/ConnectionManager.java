package com.github.reload.net.connections;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import java.net.InetSocketAddress;
import java.util.Map;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import com.github.reload.Context;
import com.github.reload.MessageBuilder;
import com.github.reload.Context.Component;
import com.github.reload.Context.CtxComponent;
import com.github.reload.net.MessageRouter;
import com.github.reload.net.encoders.Message;
import com.github.reload.net.encoders.content.AttachMessage;
import com.github.reload.net.encoders.content.ContentType;
import com.github.reload.net.encoders.content.errors.NetworkException;
import com.github.reload.net.encoders.header.DestinationList;
import com.github.reload.net.encoders.header.NodeID;
import com.github.reload.net.encoders.header.RoutableID;
import com.github.reload.net.ice.ICEHelper;
import com.github.reload.net.ice.IceCandidate;
import com.github.reload.net.ice.IceCandidate.OverlayLinkType;
import com.github.reload.net.ice.NoSuitableCandidateException;
import com.github.reload.net.server.ServerManager;
import com.github.reload.net.stack.ChannelInitializerTest;
import com.google.common.collect.Maps;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

/**
 * Establish and manage connections for all neighbor nodes
 */
public class ConnectionManager implements Component {

	private static final Logger l = Logger.getRootLogger();

	private final Map<NodeID, Connection> connections = Maps.newHashMap();

	private final Map<RoutableID, SettableFuture<Connection>> pendingConnections = Maps.newHashMap();

	@CtxComponent
	private ICEHelper iceHelper;
	@CtxComponent
	private MessageRouter msgRouter;
	@CtxComponent
	private MessageBuilder msgBuilder;
	@CtxComponent
	private ServerManager serverManager;

	private Context context;

	private NioEventLoopGroup clientLoopGroup = new NioEventLoopGroup();

	@Override
	public void compStart(Context context) {
		this.context = context;
	}

	public ListenableFuture<Connection> connect(DestinationList destList, boolean requestUpdate) {
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
		b.candidates(iceHelper.getCandidates(serverManager.getAttachServer().getLocalSocketAddress()));
		b.sendUpdate(requestUpdate);
		AttachMessage attachRequest = b.buildRequest();

		Message req = msgBuilder.newMessage(attachRequest, destList);

		l.log(Level.DEBUG, "Attach to " + destinationID + " in progress...");

		// Register pending connection request prior to send it
		pendingConnections.put(destinationID, fut);

		msgRouter.sendRequestMessage(req);

		return fut;
	}

	@Subscribe
	public void attachAnswerReceived(Message msg) throws NetworkException {
		if (msg.getContent().getType() != ContentType.ATTACH_ANS)
			return;

		AttachMessage answer = (AttachMessage) msg.getContent();

		final NodeID remoteNode = msg.getHeader().getSenderId();

		final IceCandidate remoteCandidate;

		try {
			remoteCandidate = iceHelper.testAndSelectCandidate(answer.getCandidates());
		} catch (NoSuitableCandidateException e) {
			throw new NetworkException("No suitable direct connection parameters found");
		}

		ChannelFuture chFut = connectTo(remoteCandidate.getSocketAddress(), remoteCandidate.getOverlayLinkType(), clientLoopGroup);

		chFut.addListener(new ChannelFutureListener() {

			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				Connection newConnection = new Connection(remoteCandidate.getOverlayLinkType(), future.channel(), remoteNode);
				SettableFuture<Connection> connFut = pendingConnections.get(remoteNode);
				if (connFut == null)
					return;

				connFut.set(newConnection);
				l.log(Level.DEBUG, "Attach to " + remoteNode + " at " + future.channel().remoteAddress() + " completed");
			}
		});
	}

	private ChannelFuture connectTo(InetSocketAddress remoteAddr, OverlayLinkType linkType, EventLoopGroup loopGroup) {
		Bootstrap b = new Bootstrap();
		ChannelInitializer<Channel> chInit = new ChannelInitializerTest(context, linkType);
		b.group(loopGroup).channel(NioSocketChannel.class);
		b.handler(chInit);
		return b.connect(remoteAddr);
	}

	public Connection getConnection(NodeID neighbor) {
		// TODO Auto-generated method stub
		return null;
	}
}
