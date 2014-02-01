package com.github.reload.net.data;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;
import java.util.List;

public class FrameCodec extends ByteToMessageCodec<FramedMessage> {

	@Override
	protected void encode(ChannelHandlerContext ctx, FramedMessage msg, ByteBuf out) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
		// TODO Auto-generated method stub

	}

}
