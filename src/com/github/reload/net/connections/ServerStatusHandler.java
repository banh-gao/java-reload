package com.github.reload.net.connections;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

@Sharable
public class ServerStatusHandler extends ChannelInboundHandlerAdapter {

	private final ConnectionManager mgr;

	public ServerStatusHandler(ConnectionManager mgr) {
		this.mgr = mgr;
	}

	@Override
	public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
		mgr.remoteNodeAccepted(ctx.channel());
		super.channelRegistered(ctx);
	}
}
