package com.github.reload.storage.data;

import io.netty.buffer.ByteBuf;
import com.github.reload.Configuration;
import com.github.reload.net.encoders.Codec;
import com.github.reload.net.encoders.Codec.ReloadCodec;
import com.github.reload.net.encoders.secBlock.HashAlgorithm;

/**
 * Factory class used to create objects specialized for the single data model
 * 
 */
public class SingleModel extends DataModel<SingleValue> {

	public static final String NAME = "SINGLE";

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public SingleValueBuilder newValueBuilder() {
		return new SingleValueBuilder();
	}

	@Override
	public Class<SingleValue> getValueClass() {
		return SingleValue.class;
	}

	@Override
	public SingleMetadata newMetadata(SingleValue value, HashAlgorithm hashAlg) {
		byte[] hashValue = SingleMetadata.computeHash(hashAlg, value.getValue());
		return new SingleMetadata(value.exists(), value.getValue().length, hashAlg, hashValue);
	}

	@Override
	public Class<? extends Metadata<SingleValue>> getMetadataClass() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SingleModelSpecifier newSpecifier() {
		return new SingleModelSpecifier();
	}

	@Override
	public Class<SingleModelSpecifier> getSpecifierClass() {
		return SingleModelSpecifier.class;
	}

	public static class SingleValueBuilder implements DataValueBuilder<SingleValue> {

		private byte[] value;
		private boolean exists;

		public SingleValueBuilder value(byte[] value) {
			this.value = value;
			return this;
		}

		public SingleValueBuilder exists(boolean exists) {
			this.exists = exists;
			return this;
		}

		@Override
		public SingleValue build() {
			return new SingleValue(value, exists);
		}
	}

	@ReloadCodec(SingleModelSpecifierCodec.class)
	public static class SingleModelSpecifier implements ModelSpecifier<SingleValue> {
	}

	public static class SingleModelSpecifierCodec extends Codec<SingleModelSpecifier> {

		public SingleModelSpecifierCodec(Configuration conf) {
			super(conf);
		}

		@Override
		public void encode(SingleModelSpecifier obj, ByteBuf buf, Object... params) throws CodecException {

		}

		@Override
		public SingleModelSpecifier decode(ByteBuf buf, Object... params) throws CodecException {
			return new SingleModelSpecifier();
		}

	}
}