package com.github.reload.message;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.EncoderException;
import com.github.reload.net.data.Encodable;

/**
 * RELOAD security block
 */
public class SecurityBlock implements Encodable {

	@Override
	public void encode(ByteBuf buf) throws EncoderException {
		// TODO Auto-generated method stub

	}

	public static SecurityBlock decode(ByteBuf buf) {
		// TODO Auto-generated method stub
		return null;
	}

}
