package com.github.reload.net.handlers;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import com.github.reload.net.data.ForwardMessage;

public class ForwardingHandler extends ChannelInboundHandlerAdapter {

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		// TODO Auto-generated method stub
		ForwardMessage message = (ForwardMessage) msg;
		super.channelRead(ctx, msg);
	}

}
