package com.github.reload.net.handlers;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import com.github.reload.net.data.FrameMessage;

public abstract class LinkHandler extends ChannelDuplexHandler {

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		read((FrameMessage) msg);
		// TODO Pass to upper handler
		ctx.fireChannelRead(msg);
	}

	@Override
	public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
		FrameMessage frame = write((ByteBuf) msg);
		// TODO Pass to lower handler
		ctx.write(frame);
	}

	protected abstract void read(FrameMessage message);

	protected abstract FrameMessage write(ByteBuf data);
}
