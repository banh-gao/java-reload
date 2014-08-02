package com.github.reload.net.encoders;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import java.util.List;
import org.apache.log4j.Logger;
import com.github.reload.components.ComponentsContext;
import com.github.reload.net.encoders.content.Content;
import com.github.reload.net.encoders.secBlock.SecurityBlock;

/**
 * Codec for message payload (content + security block)
 */
public class MessagePayloadDecoder extends MessageToMessageDecoder<ForwardMessage> {

	private final Codec<Content> contentCodec;
	private final Codec<SecurityBlock> secBlockCodec;

	public MessagePayloadDecoder(ComponentsContext ctx) {
		contentCodec = Codec.getCodec(Content.class, ctx);
		secBlockCodec = Codec.getCodec(SecurityBlock.class, ctx);
	}

	@Override
	protected void decode(ChannelHandlerContext ctx, ForwardMessage msg, List<Object> out) throws Exception {
		Header header = msg.getHeader();
		ByteBuf payload = msg.getPayload();
		try {
			int contentStart = payload.readerIndex();
			Content content = contentCodec.decode(payload);

			ByteBuf rawContent = payload.copy(contentStart, payload.readerIndex() - contentStart);

			SecurityBlock secBlock = secBlockCodec.decode(payload);

			Message outMsg = new Message(header, content, secBlock);

			outMsg.attributes.putAll(msg.attributes);

			outMsg.setAttribute(Message.RAW_CONTENT, rawContent);

			out.add(outMsg);
			Logger.getRootLogger().trace(String.format("Message payload %#x decoded", header.getTransactionId()));
		} finally {
			payload.release();
		}
	}
}