package com.github.reload.message;

import io.netty.buffer.ByteBuf;
import com.github.reload.Configuration;

public class DestinationListCodec extends Codec<DestinationList> {

	private final Codec<RoutableID> rouIdCodec;

	public DestinationListCodec(Configuration conf) {
		super(conf);
		rouIdCodec = getCodec(RoutableID.class);
	}

	@Override
	public void encode(DestinationList obj, ByteBuf buf, Object... params) throws com.github.reload.message.Codec.CodecException {
		for (RoutableID d : obj) {
			rouIdCodec.encode(d, buf);
		}
	}

	@Override
	public DestinationList decode(ByteBuf buf, Object... params) throws com.github.reload.message.Codec.CodecException {
		DestinationList out = new DestinationList();

		while (buf.readableBytes() > 0) {
			RoutableID id = rouIdCodec.decode(buf);
			out.add(id);
		}

		buf.release();

		return out;
	}

}
