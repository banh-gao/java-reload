package com.github.reload.message;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.EncoderException;
import com.github.reload.net.data.Encodable;

/**
 * RELOAD message content
 */
public class Content implements Encodable {

	@Override
	public void encode(ByteBuf buf) throws EncoderException {
		// TODO Auto-generated method stub
	}

	public static Content decode(ByteBuf buf) {
		// TODO Auto-generated method stub
		return null;
	}

}
