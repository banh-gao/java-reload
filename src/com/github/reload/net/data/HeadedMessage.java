package com.github.reload.net.data;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.EncoderException;
import com.github.reload.message.Header;

/**
 * RELOAD message with decoded header and opaque payload
 */
public class HeadedMessage implements Encodable {

	private Header header;
	private ByteBuf payload;

	public static HeadedMessage decode(ByteBuf in) {
		HeadedMessage message = new HeadedMessage();
		message.header = Header.decode(in);
		message.payload = in.slice();
		return message;
	}

	public Header getHeader() {
		return header;
	}

	public ByteBuf getPayload() {
		return payload;
	}

	@Override
	public void encode(ByteBuf buf) throws EncoderException {
		header.encode(buf);
		buf.writeBytes(payload, header.getPayloadLength());
	}
}
