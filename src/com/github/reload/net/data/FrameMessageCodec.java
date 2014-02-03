package com.github.reload.net.data;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;
import java.util.List;

public class FrameMessageCodec extends ByteToMessageCodec<FrameMessage> {

	@Override
	protected void encode(ChannelHandlerContext ctx, FrameMessage msg, ByteBuf out) throws Exception {
		msg.encode(out);

	}

	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
		out.add(FrameMessage.decode(in));
	}

}
