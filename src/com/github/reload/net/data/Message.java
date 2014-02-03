package com.github.reload.net.data;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.EncoderException;
import com.github.reload.message.Content;
import com.github.reload.message.Header;
import com.github.reload.message.SecurityBlock;

/**
 * A RELOAD message
 */
public class Message implements Encodable {

	private Header header;
	private Content content;
	private SecurityBlock secBlock;

	public static Message decode(HeadedMessage headedMsg) {
		Message message = new Message();
		message.header = headedMsg.getHeader();

		ByteBuf payload = headedMsg.getPayload();
		message.content = Content.decode(payload);
		message.secBlock = SecurityBlock.decode(payload);
		return message;
	}

	@Override
	public void encode(ByteBuf buf) throws EncoderException {
		int messageStart = buf.writerIndex();
		header.encode(buf);
		content.encode(buf);
		secBlock.encode(buf);

		updateMessageLength(buf, messageStart);
	}

	private void updateMessageLength(ByteBuf buf, int messageStart) {
		int oldPos = buf.writerIndex();
		int messageLength = buf.writerIndex() - messageStart;

		// Positions writerIndex to length field in the message header
		buf.writerIndex(messageStart + Header.HDR_LEADING_LEN);
		buf.writeInt(messageLength);

		// Reset original position
		buf.writerIndex(oldPos);
	}
}