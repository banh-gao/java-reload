package com.github.reload.services.storage.encoders;

import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import com.github.reload.components.ComponentsContext;
import com.github.reload.net.encoders.Codec;
import com.github.reload.net.encoders.Codec.ReloadCodec;
import com.github.reload.net.encoders.secBlock.HashAlgorithm;
import com.github.reload.services.storage.encoders.DataModel.ModelName;

/**
 * Factory class used to create objects specialized for the dictionary data
 * model
 * 
 */
@ModelName("DICT")
public class DictionaryModel extends DataModel<DictionaryValue> {

	private static final DictionaryValue NON_EXISTENT = new DictionaryValue(new byte[0], new SingleValue(new byte[0], false));

	@Override
	public DictionaryValueBuilder newValueBuilder() {
		return new DictionaryValueBuilder();
	}

	@Override
	public Class<DictionaryValue> getValueClass() {
		return DictionaryValue.class;
	}

	@Override
	public DictionaryMetadata newMetadata(DictionaryValue value, HashAlgorithm hashAlg) {
		SingleModel singleModel = getInstance(DataModel.SINGLE);
		SingleMetadata singleMeta = singleModel.newMetadata(value.getValue(), hashAlg);
		return new DictionaryMetadata(value.getKey(), singleMeta);
	}

	@Override
	public Class<? extends Metadata<DictionaryValue>> getMetadataClass() {
		return DictionaryMetadata.class;
	}

	@Override
	public DictionaryValueSpecifier newSpecifier() {
		return new DictionaryValueSpecifier();
	}

	@Override
	public Class<? extends ValueSpecifier> getSpecifierClass() {
		return DictionaryValueSpecifier.class;
	}

	@Override
	public DictionaryValue getNonExistentValue() {
		return NON_EXISTENT;
	}

	/**
	 * A dictionary prepared value created by adding a key to a single prepared
	 * value
	 * 
	 */
	public class DictionaryValueBuilder implements DataValueBuilder<DictionaryValue> {

		private final SingleValue DEFAULT_VALUE = new SingleValue(new byte[0], true);

		private byte[] key;
		private SingleValue value = DEFAULT_VALUE;

		public DictionaryValueBuilder key(byte[] key) {
			this.key = key;
			return this;
		}

		public DictionaryValueBuilder value(byte[] value, boolean exists) {
			this.value = new SingleValue(value, exists);
			return this;
		}

		@Override
		public DictionaryValue build() {
			return new DictionaryValue(key, value);
		}
	}

	/**
	 * Specifier used to fetch dictionary values
	 * 
	 */
	@ReloadCodec(DictionaryValueSpecifierCodec.class)
	public static class DictionaryValueSpecifier implements ValueSpecifier {

		List<byte[]> keys = new ArrayList<byte[]>();

		public DictionaryValueSpecifier addKey(byte[] key) {
			if (key == null)
				throw new NullPointerException();
			keys.add(key);
			return this;
		}

		public List<byte[]> getKeys() {
			return keys;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			DictionaryValueSpecifier other = (DictionaryValueSpecifier) obj;
			if (keys == null) {
				if (other.keys != null)
					return false;
			} else if (!keys.equals(other.keys))
				return false;
			return true;
		}

		@Override
		public int hashCode() {
			return Objects.hash(super.hashCode(), keys);
		}

		@Override
		public boolean isMatching(DataValue value) {
			if (!(value instanceof DictionaryValue))
				return false;

			DictionaryValue v = (DictionaryValue) value;

			if (!v.getValue().exists())
				return false;

			for (byte[] k : getKeys()) {
				if (Arrays.equals(k, v.getKey()))
					return true;
			}

			return false;
		}

	}

	static class DictionaryValueSpecifierCodec extends Codec<DictionaryValueSpecifier> {

		private static final int KEYS_LENGTH_FIELD = U_INT16;
		private static final int KEY_ENTRY_FIELD = U_INT16;

		public DictionaryValueSpecifierCodec(ComponentsContext ctx) {
			super(ctx);
		}

		@Override
		public void encode(DictionaryValueSpecifier obj, ByteBuf buf, Object... params) throws CodecException {
			Field lenFld = allocateField(buf, KEYS_LENGTH_FIELD);

			for (byte[] k : obj.keys) {
				Field entryFld = allocateField(buf, KEY_ENTRY_FIELD);
				buf.writeBytes(k);
				entryFld.updateDataLength();
			}

			lenFld.updateDataLength();
		}

		@Override
		public DictionaryValueSpecifier decode(ByteBuf buf, Object... params) throws CodecException {
			ByteBuf keysBuf = readField(buf, KEYS_LENGTH_FIELD);

			DictionaryValueSpecifier spec = new DictionaryValueSpecifier();

			while (keysBuf.readableBytes() > 0) {
				ByteBuf keyFld = readField(keysBuf, KEY_ENTRY_FIELD);
				byte[] value = new byte[keyFld.readableBytes()];
				keyFld.readBytes(value);
				spec.addKey(value);
				keyFld.release();
			}

			keysBuf.release();

			return spec;
		}
	}
}