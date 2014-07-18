package com.github.reload.net.encoders.secBlock;

import io.netty.buffer.ByteBuf;
import com.github.reload.Configuration;
import com.github.reload.net.encoders.Codec;
import com.github.reload.net.encoders.Codec.ReloadCodec;
import com.github.reload.net.encoders.secBlock.NoneSignerIndentityValue.NoneSignerIdentityValueCodec;

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

		public NoneSignerIdentityValueCodec(Configuration conf) {
			super(conf);
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