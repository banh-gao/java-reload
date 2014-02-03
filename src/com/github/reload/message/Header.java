package com.github.reload.message;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.EncoderException;
import com.github.reload.net.data.Encodable;

/**
 * RELOAD message header
 */
public class Header implements Encodable {

	/**
	 * Size in bytes of the first part of the header from the beginning to the
	 * message length field
	 */
	public static int HDR_LEADING_LEN = 128;

	public int getPayloadLength() {
		// TODO: return payload length
		return 0;
	}

	public static Header decode(ByteBuf buf) {
		// TODO : decode header
		return null;
	}

	@Override
	public void encode(ByteBuf buf) throws EncoderException {
		// TODO : encode header
	}

}
