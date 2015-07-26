package com.github.reload.services.storage.encoders;

import io.netty.buffer.ByteBuf;
import com.github.reload.components.ComponentsContext;
import com.github.reload.net.encoders.Codec;
import com.github.reload.net.encoders.Codec.ReloadCodec;
import com.github.reload.net.encoders.secBlock.HashAlgorithm;
import com.github.reload.services.storage.encoders.DataModel.ModelName;

/**
 * Factory class used to create objects specialized for the single data model
 * 
 */
@ModelName("SINGLE")
public class SingleModel extends DataModel<SingleValue> {

	private static final SingleValue NON_EXISTENT = new SingleValue(new byte[0], false);

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
	public SingleValueSpecifier newSpecifier() {
		return new SingleValueSpecifier();
	}

	@Override
	public Class<SingleValueSpecifier> getSpecifierClass() {
		return SingleValueSpecifier.class;
	}

	@Override
	public SingleValue getNonExistentValue() {
		return NON_EXISTENT;
	}

	public static class SingleValueBuilder implements DataValueBuilder<SingleValue> {

		private byte[] value;
		private boolean exists = true;

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
			if (value == null) {
				value = new byte[0];
			}
			return new SingleValue(value, exists);
		}
	}

	@ReloadCodec(SingleValueSpecifierCodec.class)
	public static class SingleValueSpecifier implements ValueSpecifier {

		@Override
		public boolean isMatching(DataValue value) {
			return value instanceof SingleValue;
		}

	}

	static class SingleValueSpecifierCodec extends Codec<SingleValueSpecifier> {

		public SingleValueSpecifierCodec(ComponentsContext ctx) {
			super(ctx);
		}

		@Override
		public void encode(SingleValueSpecifier obj, ByteBuf buf, Object... params) throws CodecException {

		}

		@Override
		public SingleValueSpecifier decode(ByteBuf buf, Object... params) throws CodecException {
			return new SingleValueSpecifier();
		}

	}
}