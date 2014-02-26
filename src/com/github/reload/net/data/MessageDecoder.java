package com.github.reload.net.data;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import java.util.List;
import com.github.reload.Configuration;
import com.github.reload.message.Content;
import com.github.reload.message.Header;
import com.github.reload.message.SecurityBlock;

/**
 * Completely decode the message payload for a HeadedMessage
 */
public class MessageDecoder extends MessageToMessageDecoder<HeadedMessage> {

	private final Codec<Content> contentCodec;
	private final Codec<SecurityBlock> secBlockCodec;

	public MessageDecoder(Configuration conf) {
		contentCodec = Codec.getCodec(Content.class, conf);
		secBlockCodec = Codec.getCodec(SecurityBlock.class, conf);
	}

	@Override
	protected void decode(ChannelHandlerContext ctx, HeadedMessage msg, List<Object> out) throws Exception {
		Header header = msg.getHeader();
		ByteBuf payload = msg.getPayload();
		try {
			Content content = contentCodec.decode(payload);
			SecurityBlock secBlock = secBlockCodec.decode(payload);
			out.add(new Message(header, content, secBlock));
		} finally {
			payload.release();
		}
	}
}