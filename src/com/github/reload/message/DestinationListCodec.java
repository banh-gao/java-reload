package com.github.reload.message;

import io.netty.buffer.ByteBuf;
import com.github.reload.Context;
import com.github.reload.net.data.Codec;

public class DestinationListCodec extends Codec<DestinationList> {

	public DestinationListCodec(Context context) {
		super(context);
	}

	@Override
	public void encode(DestinationList obj, ByteBuf buf) {
		for (RoutableID d : obj) {
			d.writeAsDestinationTo(buf);
		}
	}

	@Override
	public DestinationList decode(ByteBuf buf) {
		DestinationList out = new DestinationList();

		while (buf.readableBytes() > 0) {
			RoutableID id = RoutableID.parseFromDestination(buf);
			out.add(id);
		}

		return out;
	}

}
