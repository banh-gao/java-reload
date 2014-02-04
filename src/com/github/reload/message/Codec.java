package com.github.reload.message;

import io.netty.buffer.ByteBuf;
import com.github.reload.Context;

/**
 * Encode and decode the object on the given buffer
 * 
 * @param <T>
 *            The object type handled by this codec
 */
public interface Codec<T> {

	/**
	 * Initialize codec
	 * 
	 * @param ctx
	 * @param factory
	 */
	public void init(Context ctx, CodecFactory factory);

	/**
	 * Encode object to the given byte buffer
	 * 
	 * @param data
	 * @param buf
	 */
	public void encode(T obj, ByteBuf buf);

	/**
	 * Decode object from the given byte buffer
	 * 
	 * @param buf
	 * @return
	 */
	public T decode(ByteBuf buf);

}