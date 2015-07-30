package com.github.reload.net.codecs.secBlock;

import io.netty.buffer.ByteBuf;
import dagger.ObjectGraph;
import com.github.reload.net.codecs.Codec;
import com.github.reload.net.codecs.Codec.ReloadCodec;
import com.github.reload.net.codecs.secBlock.NoneSignerIndentityValue.NoneSignerIdentityValueCodec;

@ReloadCodec(NoneSignerIdentityValueCodec.class)
class NoneSignerIndentityValue extends SignerIdentityValue {

	@Override
	public HashAlgorithm getHashAlgorithm() {
		return HashAlgorithm.NONE;
	}

	@Override
	public byte[] getHashValue() {
		return new byte[0];
	}

	static class NoneSignerIdentityValueCodec extends Codec<NoneSignerIndentityValue> {

		public NoneSignerIdentityValueCodec(ObjectGraph ctx) {
			super(ctx);
		}

		@Override
		public void encode(NoneSignerIndentityValue obj, ByteBuf buf, Object... params) throws CodecException {
			// No data
		}

		@Override
		public NoneSignerIndentityValue decode(ByteBuf buf, Object... params) throws CodecException {
			return new NoneSignerIndentityValue();
		}

	}

}