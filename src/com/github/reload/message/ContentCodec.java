package com.github.reload.message;

import io.netty.buffer.ByteBuf;
import com.github.reload.Context;
import com.github.reload.net.data.Codec;

public class ContentCodec extends Codec<Content> {

	public ContentCodec(Context context) {
		super(context);
	}

	@Override
	public void encode(Content obj, ByteBuf buf) throws com.github.reload.net.data.Codec.CodecException {
		// TODO Auto-generated method stub

	}

	@Override
	public Content decode(ByteBuf buf) throws com.github.reload.net.data.Codec.CodecException {
		// TODO Auto-generated method stub
		return null;
	}

}