package com.github.reload.message;

import io.netty.buffer.ByteBuf;
import com.github.reload.Context;
import com.github.reload.message.SignerIdentity.IdentityType;
import com.github.reload.message.SignerIdentityValue.SignerIdentityValueCodec;
import com.github.reload.net.data.Codec;
import com.github.reload.net.data.ReloadCodec;

@ReloadCodec(SignerIdentityValueCodec.class)
public abstract class SignerIdentityValue {

	public static class SignerIdentityValueCodec extends Codec<SignerIdentityValue> {

		public SignerIdentityValueCodec(Context context) {
			super(context);
		}

		@Override
		public void encode(SignerIdentityValue obj, ByteBuf buf, Object... params) throws CodecException {
			@SuppressWarnings("unchecked")
			Codec<SignerIdentityValue> codec = (Codec<SignerIdentityValue>) getCodec(obj.getClass());
			codec.encode(obj, buf);
		}

		@Override
		public SignerIdentityValue decode(ByteBuf buf, Object... params) throws CodecException {
			if (params.length < 1 || !(params[0] instanceof IdentityType))
				throw new IllegalArgumentException("Signer identity type needed to decode identity value");

			Class<? extends SignerIdentityValue> valueClass = ((IdentityType) params[0]).getValueClass();

			@SuppressWarnings("unchecked")
			Codec<SignerIdentityValue> valueCodec = (Codec<SignerIdentityValue>) getCodec(valueClass);

			return valueCodec.decode(buf);
		}

	}
}