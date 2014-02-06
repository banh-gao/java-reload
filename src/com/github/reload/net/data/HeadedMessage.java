package com.github.reload.net.data;

import io.netty.buffer.ByteBuf;
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
}
