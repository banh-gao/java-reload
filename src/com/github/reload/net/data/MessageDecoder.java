package com.github.reload.net.data;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import java.util.List;
import com.github.reload.message.MessageCodec;

/**
 * Completely decode the message payload for a HeadedMessage
 */
public class MessageDecoder extends MessageToMessageDecoder<HeadedMessage> {

	private MessageCodec codec = new MessageCodec();

	@Override
	protected void decode(ChannelHandlerContext ctx, HeadedMessage msg, List<Object> out) throws Exception {
		out.add(codec.decode(msg));
	}
}