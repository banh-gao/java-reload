package com.github.reload.net.connections;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import java.net.InetSocketAddress;
import com.github.reload.Context;
import com.github.reload.net.ice.IceCandidate.OverlayLinkType;

public class ChannelFactory {

	public static ChannelFuture connectTo(InetSocketAddress remoteAddr, OverlayLinkType linkType, EventLoopGroup loopGroup, Context context) {
		Bootstrap b = new Bootstrap();
		ChannelHandler chHandler = newChannelHandler(context, linkType);
		b.group(loopGroup).channel(NioSocketChannel.class);
		b.handler(chHandler);
		return b.connect(remoteAddr);
	}

	public static ChannelFuture bind(InetSocketAddress localAddr, OverlayLinkType linkType, EventLoopGroup loopGroup, Context context) {
		ServerBootstrap sb = new ServerBootstrap();
		ChannelHandler chHandler = newChannelHandler(context, linkType);
		sb.group(loopGroup).channel(NioServerSocketChannel.class);
		sb.childHandler(chHandler);
		sb.childOption(ChannelOption.SO_KEEPALIVE, true);
		return sb.bind(localAddr);
	}

	private static ChannelHandler newChannelHandler(Context context, OverlayLinkType linkType) {
		return new ChannelInitializerImpl(context, linkType);
	}

}
