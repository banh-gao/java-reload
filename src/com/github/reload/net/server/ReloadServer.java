package com.github.reload.net.server;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import java.net.InetSocketAddress;
import com.github.reload.Context;
import com.github.reload.net.connections.ChannelFactory;
import com.github.reload.net.ice.IceCandidate.OverlayLinkType;
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
		ChannelFactory.bind(localAddr, linkType, handlersLoopGroup, context).await();
	}

	@Override
	protected void shutDown() throws Exception {
		handlersLoopGroup.shutdownGracefully().await();
	}

}
