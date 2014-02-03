package com.github.reload.net.data;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import java.util.List;

/**
 * Completely decode the message payload for a HeadedMessage
 */
public class MessageDecoder extends MessageToMessageDecoder<HeadedMessage> {

	@Override
	protected void decode(ChannelHandlerContext ctx, HeadedMessage msg, List<Object> out) throws Exception {
		out.add(Message.decode(msg));
	}
}