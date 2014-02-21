package com.github.reload.message;

import io.netty.buffer.ByteBuf;
import com.github.reload.Context;
import com.github.reload.message.UnknownForwardingOption.UnknownForwardingOptionCodec;
import com.github.reload.net.data.Codec;
import com.github.reload.net.data.ReloadCodec;

@ReloadCodec(UnknownForwardingOptionCodec.class)
public class UnknownForwardingOption extends ForwardingOption {

	byte[] data;

	@Override
	protected ForwardingOptionType getType() {
		return ForwardingOptionType.UNKNOWN_OPTION;
	}

	public static class UnknownForwardingOptionCodec extends Codec<UnknownForwardingOption> {

		public UnknownForwardingOptionCodec(Context context) {
			super(context);
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