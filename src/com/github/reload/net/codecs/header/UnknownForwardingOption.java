package com.github.reload.net.codecs.header;

import io.netty.buffer.ByteBuf;
import com.github.reload.components.ComponentsContext;
import com.github.reload.net.codecs.Codec;
import com.github.reload.net.codecs.Codec.ReloadCodec;
import com.github.reload.net.codecs.header.UnknownForwardingOption.UnknownForwardingOptionCodec;

@ReloadCodec(UnknownForwardingOptionCodec.class)
public class UnknownForwardingOption extends ForwardingOption {

	byte[] data;

	@Override
	public ForwardingOptionType getType() {
		return ForwardingOptionType.UNKNOWN_OPTION;
	}

	static class UnknownForwardingOptionCodec extends Codec<UnknownForwardingOption> {

		public UnknownForwardingOptionCodec(ComponentsContext ctx) {
			super(ctx);
		}

		@Override
		public void encode(UnknownForwardingOption obj, ByteBuf buf, Object... params) {
			if (obj.data != null) {
				buf.writeBytes(obj.data);
			}
		}

		@Override
		public UnknownForwardingOption decode(ByteBuf buf, Object... params) {
			UnknownForwardingOption obj = new UnknownForwardingOption();
			obj.data = new byte[buf.readableBytes()];
			buf.readBytes(obj.data);
			buf.release();
			return obj;
		}

	}
}