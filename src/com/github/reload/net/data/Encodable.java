package com.github.reload.net.data;

import io.netty.buffer.ByteBuf;

/**
 * The implementors declare to be encodable to a byte buffer
 */
public interface Encodable {

	/**
	 * Encode the object on the given buffer
	 * 
	 * @param buf
	 */
	public void encode(ByteBuf buf);
}
