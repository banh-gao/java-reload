package com.github.reload.message;

import io.netty.buffer.ByteBuf;
import com.github.reload.Context;
import com.github.reload.net.data.HeadedMessage;

/**
 * Codec for a RELOAD message. The decoder part uses a message with the header
 * already decoded
 */
public class MessageCodec {

	private CodecFactory factory;

	public void init(Context ctx, CodecFactory factory) {
		this.factory = factory;
	}

	public void encode(Message obj, ByteBuf buf) {
		int messageStart = buf.writerIndex();
		factory.getCodec(Header.class).encode(obj.header, buf);
		factory.getCodec(Content.class).encode(obj.content, buf);
		factory.getCodec(SecurityBlock.class).encode(obj.secBlock, buf);

		updateMessageLength(buf, messageStart);
	}

	private void updateMessageLength(ByteBuf buf, int messageStart) {
		int messageLength = buf.writerIndex() - messageStart;
		buf.setInt(messageStart + HeaderCodec.HDR_LEADING_LEN, messageLength);
	}

	public Message decode(HeadedMessage headedMsg) {
		Message message = new Message();
		message.header = headedMsg.getHeader();

		ByteBuf payload = headedMsg.getPayload();
		message.content = factory.getCodec(Content.class).decode(payload);
		message.secBlock = factory.getCodec(SecurityBlock.class).decode(payload);
		return message;
	}

}
