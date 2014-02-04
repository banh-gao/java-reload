package com.github.reload.message;

import io.netty.buffer.ByteBuf;

public class DestinationListCodec extends AbstractCodec<DestinationList> {

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
