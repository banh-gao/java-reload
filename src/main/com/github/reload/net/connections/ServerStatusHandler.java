package com.github.reload.net.connections;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

@Sharable
public class ServerStatusHandler extends ChannelInboundHandlerAdapter {

	private ConnectionManager connMgr;

	public ServerStatusHandler(ConnectionManager connectionManager) {
		this.connMgr = connectionManager;
	}

	public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
		connMgr.clientConnected(ctx.channel());
		super.channelRegistered(ctx);
	}

	public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
		connMgr.clientDisonnected(ctx.channel());
	}
}
