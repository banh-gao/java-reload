package com.github.reload.net.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import java.net.InetSocketAddress;
import com.github.reload.Context;
import com.github.reload.net.ice.IceCandidate.OverlayLinkType;
import com.github.reload.net.stack.ChannelInitializerTest;
import com.google.common.util.concurrent.AbstractIdleService;

public class ReloadServer extends AbstractIdleService {

	private final Context context;
	private final InetSocketAddress localAddr;
	private final OverlayLinkType linkType;
	private final EventLoopGroup handlersLoopGroup = new NioEventLoopGroup();

	public ReloadServer(Context context, InetSocketAddress localAddr, OverlayLinkType linkType) {
		this.context = context;
		this.localAddr = localAddr;
		this.linkType = linkType;
	}

	@Override
	protected void startUp() throws Exception {
		bind(localAddr, linkType, handlersLoopGroup, context).await();
	}

	private ChannelFuture bind(InetSocketAddress localAddr, OverlayLinkType linkType, EventLoopGroup loopGroup, Context context) {
		ServerBootstrap sb = new ServerBootstrap();
		ChannelHandler chHandler = new ChannelInitializerTest(context, linkType);
		sb.group(loopGroup).channel(NioServerSocketChannel.class);
		sb.childHandler(chHandler);
		sb.childOption(ChannelOption.SO_KEEPALIVE, true);
		return sb.bind(localAddr);
	}

	@Override
	protected void shutDown() throws Exception {
		handlersLoopGroup.shutdownGracefully().await();
	}

	public InetSocketAddress getLocalSocketAddress() {
		return localAddr;
	}

}
