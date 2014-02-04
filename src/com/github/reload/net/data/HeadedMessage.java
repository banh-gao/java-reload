package com.github.reload.net.data;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.EncoderException;
import com.github.reload.message.Header;

/**
 * RELOAD message with decoded header and opaque payload
 */
public class HeadedMessage {

	Header header;
	ByteBuf payload;

	public Header getHeader() {
		return header;
	}

	public ByteBuf getPayload() {
		return payload;
	}

	public void encode(ByteBuf buf) throws EncoderException {
		header.encode(buf);
		buf.writeBytes(payload, header.getPayloadLength());
	}
}
