package com.github.reload.net.data;

import io.netty.buffer.ByteBuf;
import com.github.reload.Context;
import com.github.reload.message.Content;
import com.github.reload.message.Header;
import com.github.reload.message.HeaderCodec;
import com.github.reload.message.SecurityBlock;
import com.github.reload.net.data.Codec.CodecException;

/**
 * Base Codec for a RELOAD message. The decoder part uses a message with
 * the header already decoded
 */
public class MessageCodec {

	private final Codec<Header> hdrCodec;
	private final Codec<Content> contentCodec;
	private final Codec<SecurityBlock> secBlockCodec;

	public MessageCodec(Context context) {
		hdrCodec = Codec.getCodec(Header.class, context);
		contentCodec = Codec.getCodec(Content.class, context);
		secBlockCodec = Codec.getCodec(SecurityBlock.class, context);
	}

	public void encode(Message obj, ByteBuf buf) throws CodecException {
		int messageStart = buf.writerIndex();

		hdrCodec.encode(obj.header, buf);
		contentCodec.encode(obj.content, buf);
		secBlockCodec.encode(obj.secBlock, buf);

		updateMessageLength(buf, messageStart);
	}

	private void updateMessageLength(ByteBuf buf, int messageStart) {
		int messageLength = buf.writerIndex() - messageStart;
		buf.setInt(messageStart + HeaderCodec.HDR_LEADING_LEN, messageLength);
	}

	public Message decode(HeadedMessage headedMsg) throws CodecException {
		Message message = new Message();
		message.header = headedMsg.getHeader();

		ByteBuf payload = headedMsg.getPayload();
		message.content = contentCodec.decode(payload);
		message.secBlock = secBlockCodec.decode(payload);
		return message;
	}
}
