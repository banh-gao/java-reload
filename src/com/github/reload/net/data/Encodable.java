package com.github.reload.net.data;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.EncoderException;

/**
 * The implementors declare to be encodable to a byte buffer
 */
public interface Encodable {

	/**
	 * Encode the object on the given buffer
	 * 
	 * @param buf
	 */
	public void encode(ByteBuf buf) throws EncoderException;
}
