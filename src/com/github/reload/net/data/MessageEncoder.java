package com.github.reload.net.data;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import com.github.reload.message.Message;
import com.github.reload.message.MessageCodec;

/**
 * Encode the message to be exchanged on the RELOAD overlay
 */
public class MessageEncoder extends MessageToByteEncoder<Message> {

	private MessageCodec codec = new MessageCodec();

	@Override
	protected void encode(ChannelHandlerContext ctx, Message msg, ByteBuf out) throws Exception {
		codec.encode(msg, out);
	}

}
