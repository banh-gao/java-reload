package com.github.reload.net.connections;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import java.net.InetSocketAddress;
import com.github.reload.Context;
import com.github.reload.net.ice.IceCandidate.OverlayLinkType;

public class ClientChannelFactory {

	public static ChannelFuture connectTo(InetSocketAddress remoteAddr, OverlayLinkType linkType, EventLoopGroup loopGroup, Context context) {
		Bootstrap b = new Bootstrap();
		ChannelHandler chHandler = newChannelHandler(context, linkType);
		b.group(loopGroup).channel(NioSocketChannel.class).handler(chHandler);
		return b.connect(remoteAddr);
	}

	private static ChannelHandler newChannelHandler(Context context, OverlayLinkType linkType) {
		return new ChannelInitializerImpl(context, linkType);
	}
}
