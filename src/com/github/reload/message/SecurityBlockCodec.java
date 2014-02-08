package com.github.reload.message;

import io.netty.buffer.ByteBuf;
import com.github.reload.Context;
import com.github.reload.net.data.Codec;

public class SecurityBlockCodec extends Codec<SecurityBlock> {

	public SecurityBlockCodec(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void encode(SecurityBlock obj, ByteBuf buf) throws com.github.reload.net.data.Codec.CodecException {
		// TODO Auto-generated method stub

	}

	@Override
	public SecurityBlock decode(ByteBuf buf) throws com.github.reload.net.data.Codec.CodecException {
		// TODO Auto-generated method stub
		return null;
	}

}
