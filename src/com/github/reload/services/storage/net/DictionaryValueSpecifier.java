package com.github.reload.services.storage.net;

import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import dagger.ObjectGraph;
import com.github.reload.net.codecs.Codec;
import com.github.reload.net.codecs.Codec.ReloadCodec;
import com.github.reload.services.storage.DataModel;
import com.github.reload.services.storage.DataModel.DataValue;
import com.github.reload.services.storage.DataModel.ValueSpecifier;
import com.github.reload.services.storage.net.DictionaryValueSpecifier.DictionaryValueSpecifierCodec;

/**
 * Specifier used to fetch dictionary values
 * 
 */
@ReloadCodec(DictionaryValueSpecifierCodec.class)
public class DictionaryValueSpecifier implements ValueSpecifier {

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

	static class DictionaryValueSpecifierCodec extends Codec<DictionaryValueSpecifier> {

		private static final int KEYS_LENGTH_FIELD = U_INT16;
		private static final int KEY_ENTRY_FIELD = U_INT16;

		public DictionaryValueSpecifierCodec(ObjectGraph ctx) {
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