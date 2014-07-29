package com.github.reload.storage.encoders;

import io.netty.buffer.ByteBuf;
import com.github.reload.conf.Configuration;
import com.github.reload.net.encoders.Codec;
import com.github.reload.net.encoders.Codec.ReloadCodec;
import com.github.reload.net.encoders.secBlock.HashAlgorithm;
import com.github.reload.storage.DataModel;
import com.github.reload.storage.DataModel.ModelName;

/**
 * Factory class used to create objects specialized for the single data model
 * 
 */
@ModelName("SINGLE")
public class SingleModel extends DataModel<SingleValue> {

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
		return SingleMetadata.class;
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

		@Override
		public boolean isMatching(SingleValue value) {
			return true;
		}

	}

	static class SingleModelSpecifierCodec extends Codec<SingleModelSpecifier> {

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