package com.github.reload.message;

import io.netty.buffer.ByteBuf;
import com.github.reload.Context;
import com.github.reload.net.data.Codec;

public class DestinationListCodec extends Codec<DestinationList> {

	private final Codec<RoutableID> rouIdCodec;

	public DestinationListCodec(Context context) {
		super(context);
		rouIdCodec = getCodec(RoutableID.class, context);
	}

	@Override
	public void encode(DestinationList obj, ByteBuf buf) throws com.github.reload.net.data.Codec.CodecException {
		for (RoutableID d : obj) {
			rouIdCodec.encode(d, buf);
		}
	}

	@Override
	public DestinationList decode(ByteBuf buf) throws com.github.reload.net.data.Codec.CodecException {
		DestinationList out = new DestinationList();

		while (buf.readableBytes() > 0) {
			RoutableID id = rouIdCodec.decode(buf);
			out.add(id);
		}

		return out;
	}

}
