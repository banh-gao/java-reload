package com.github.reload.storage.data;

import io.netty.buffer.ByteBuf;
import com.github.reload.Context;
import com.github.reload.net.data.Codec;
import com.github.reload.net.data.ReloadCodec;

/**
 * Factory class used to create objects specialized for the single data model
 * 
 */
public class SingleModel extends DataModel {

	private static final String NAME = "SINGLE";

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public DataValueBuilder newValueBuilder() {
		return new SingleValueBuilder();
	}

	@Override
	public ModelSpecifier newSpecifier() {
		return new SingleModelSpecifier();
	}

	@Override
	public Class<? extends ModelSpecifier> getSpecifierClass() {
		return ModelSpecifier.class;
	}

	public static class SingleValueBuilder implements DataValueBuilder {

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
	public static class SingleModelSpecifier implements ModelSpecifier {
	}

	public static class SingleModelSpecifierCodec extends Codec<ModelSpecifier> {

		public SingleModelSpecifierCodec(Context context) {
			super(context);
		}

		@Override
		public void encode(ModelSpecifier obj, ByteBuf buf, Object... params) throws CodecException {

		}

		@Override
		public ModelSpecifier decode(ByteBuf buf, Object... params) throws CodecException {
			return new SingleModelSpecifier();
		}

	}
}