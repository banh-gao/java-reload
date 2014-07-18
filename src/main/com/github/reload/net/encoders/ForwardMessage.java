package com.github.reload.net.encoders;

import io.netty.buffer.ByteBuf;
import com.github.reload.net.encoders.header.Header;

/**
 * RELOAD message with decoded header and opaque payload
 */
public class ForwardMessage {

	Header header;
	ByteBuf payload;

	public Header getHeader() {
		return header;
	}

	public ByteBuf getPayload() {
		return payload;
	}
}
