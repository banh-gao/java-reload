package com.github.reload.net.data;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;
import java.util.List;
import com.github.reload.Context;
import com.github.reload.message.Header;

/**
 * Decoder used to process only the Forwarding Header part of the message
 */
public class HeadedMessageCodec extends ByteToMessageCodec<HeadedMessage> {

	private final Codec<Header> hdrCodec;

	public HeadedMessageCodec(Context ctx) {
		this.hdrCodec = Codec.getCodec(Header.class, ctx);
	}

	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
		HeadedMessage message = new HeadedMessage();
		message.header = hdrCodec.decode(in);
		message.payload = in.slice();
		out.add(message);
	}

	@Override
	protected void encode(ChannelHandlerContext ctx, HeadedMessage msg, ByteBuf out) throws Exception {
		hdrCodec.encode(msg.header, out);
		out.writeBytes(msg.getPayload(), msg.header.getPayloadLength());
	}
}
