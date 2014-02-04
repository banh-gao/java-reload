package com.github.reload.net.data;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;
import java.util.List;

/**
 * Codec for RELOAD frame messages exchanged on a link to a neighbor node
 */
public class FramedMessageCodec extends ByteToMessageCodec<FramedMessage> {

	@Override
	protected void encode(ChannelHandlerContext ctx, FramedMessage msg, ByteBuf out) throws Exception {
		msg.encode(out);
	}

	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
		out.add(FramedMessage.decode(in));
	}
}
