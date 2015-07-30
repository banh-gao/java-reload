package com.github.reload.net.codecs.secBlock;

import io.netty.buffer.ByteBuf;
import java.util.Arrays;
import java.util.Objects;
import dagger.ObjectGraph;
import com.github.reload.net.codecs.Codec;
import com.github.reload.net.codecs.Codec.ReloadCodec;
import com.github.reload.net.codecs.secBlock.SignerIdentity.IdentityType;
import com.github.reload.net.codecs.secBlock.SignerIdentityValue.SignerIdentityValueCodec;

@ReloadCodec(SignerIdentityValueCodec.class)
public abstract class SignerIdentityValue {

	public abstract HashAlgorithm getHashAlgorithm();

	public abstract byte[] getHashValue();

	static class SignerIdentityValueCodec extends Codec<SignerIdentityValue> {

		public SignerIdentityValueCodec(ObjectGraph ctx) {
			super(ctx);
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

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), getHashAlgorithm(), getHashValue());
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SignerIdentityValue other = (SignerIdentityValue) obj;
		if (getHashAlgorithm() != other.getHashAlgorithm())
			return false;
		if (!Arrays.equals(getHashValue(), other.getHashValue()))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "SignerIdentityValue [hashAlgorithm=" + getHashAlgorithm() + ", hashValue=" + Arrays.toString(getHashValue()) + "]";
	}

}