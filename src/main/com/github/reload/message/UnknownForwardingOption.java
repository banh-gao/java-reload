package com.github.reload.message;

import io.netty.buffer.ByteBuf;
import com.github.reload.Configuration;
import com.github.reload.message.Codec.ReloadCodec;
import com.github.reload.message.UnknownForwardingOption.UnknownForwardingOptionCodec;

@ReloadCodec(UnknownForwardingOptionCodec.class)
public class UnknownForwardingOption extends ForwardingOption {

	byte[] data;

	@Override
	protected ForwardingOptionType getType() {
		return ForwardingOptionType.UNKNOWN_OPTION;
	}

	public static class UnknownForwardingOptionCodec extends Codec<UnknownForwardingOption> {

		public UnknownForwardingOptionCodec(Configuration conf) {
			super(conf);
		}

		@Override
		public void encode(UnknownForwardingOption obj, ByteBuf buf, Object... params) {
			buf.writeBytes(obj.data);
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