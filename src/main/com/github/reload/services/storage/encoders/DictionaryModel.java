package com.github.reload.services.storage.encoders;

import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import com.github.reload.components.ComponentsContext;
import com.github.reload.net.encoders.Codec;
import com.github.reload.net.encoders.Codec.ReloadCodec;
import com.github.reload.net.encoders.secBlock.HashAlgorithm;
import com.github.reload.services.storage.DataModel;
import com.github.reload.services.storage.DataModel.ModelName;
import com.github.reload.services.storage.encoders.DictionaryValue.Key;

/**
 * Factory class used to create objects specialized for the dictionary data
 * model
 * 
 */
@ModelName("DICT")
public class DictionaryModel extends DataModel<DictionaryValue> {

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
	public DictionaryModelSpecifier newSpecifier() {
		return new DictionaryModelSpecifier();
	}

	@Override
	public Class<? extends ModelSpecifier> getSpecifierClass() {
		return DictionaryModelSpecifier.class;
	}

	/**
	 * A dictionary prepared value created by adding a key to a single prepared
	 * value
	 * 
	 */
	public class DictionaryValueBuilder implements DataValueBuilder<DictionaryValue> {

		private Key key;
		private SingleValue value;

		public DictionaryValueBuilder key(Key key) {
			this.key = key;
			return this;
		}

		public DictionaryValueBuilder value(SingleValue value) {
			this.value = value;
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
	@ReloadCodec(DictionaryModelSpecifierCodec.class)
	public static class DictionaryModelSpecifier implements ModelSpecifier {

		List<Key> keys = new ArrayList<DictionaryValue.Key>();

		public void addKey(byte[] key) {
			if (key == null)
				throw new NullPointerException();
			keys.add(new Key(key));
		}

		public List<Key> getKeys() {
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
			DictionaryModelSpecifier other = (DictionaryModelSpecifier) obj;
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

			for (Key k : getKeys()) {
				if (k.equals(v.getKey()))
					return true;
			}

			return false;
		}

	}

	static class DictionaryModelSpecifierCodec extends Codec<DictionaryModelSpecifier> {

		private static final int KEYS_LENGTH_FIELD = U_INT16;
		private static final int KEY_ENTRY_FIELD = U_INT16;

		public DictionaryModelSpecifierCodec(ComponentsContext ctx) {
			super(ctx);
		}

		@Override
		public void encode(DictionaryModelSpecifier obj, ByteBuf buf, Object... params) throws CodecException {
			Field lenFld = allocateField(buf, KEYS_LENGTH_FIELD);

			for (Key k : obj.keys) {
				buf.writeBytes(k.getValue());
			}

			lenFld.updateDataLength();
		}

		@Override
		public DictionaryModelSpecifier decode(ByteBuf buf, Object... params) throws CodecException {
			ByteBuf keysBuf = readField(buf, KEYS_LENGTH_FIELD);

			DictionaryModelSpecifier spec = new DictionaryModelSpecifier();

			while (keysBuf.readableBytes() > 0) {
				ByteBuf keyFld = readField(keysBuf, KEY_ENTRY_FIELD);
				byte[] value = new byte[keyFld.readableBytes()];
				spec.addKey(value);
			}

			return spec;
		}

	}
}