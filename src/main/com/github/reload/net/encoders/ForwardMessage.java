package com.github.reload.net.encoders;

import io.netty.buffer.ByteBuf;
import io.netty.util.AttributeKey;
import java.util.Map;
import com.github.reload.net.encoders.header.Header;
import com.google.common.collect.Maps;

/**
 * RELOAD message with decoded header and opaque payload
 */
public class ForwardMessage {

	final Map<AttributeKey<?>, Object> attributes = Maps.newHashMap();

	Header header;
	ByteBuf payload;

	public Header getHeader() {
		return header;
	}

	public ByteBuf getPayload() {
		return payload;
	}

	@SuppressWarnings("unchecked")
	public <T> T setAttribute(AttributeKey<T> key, T value) {
		return (T) attributes.put(key, value);
	}

	@SuppressWarnings("unchecked")
	public <T> T getAttribute(AttributeKey<T> key) {
		Object a = attributes.get(key);
		if (a != null)
			return (T) a;
		return null;
	}
}
