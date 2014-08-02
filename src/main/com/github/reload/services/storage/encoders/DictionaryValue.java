package com.github.reload.services.storage.encoders;

import io.netty.buffer.ByteBuf;
import java.util.Arrays;
import com.github.reload.components.ComponentsContext;
import com.github.reload.net.encoders.Codec;
import com.github.reload.net.encoders.Codec.ReloadCodec;
import com.github.reload.services.storage.DataModel.DataValue;
import com.github.reload.services.storage.encoders.DictionaryValue.DictionaryValueCodec;
import com.google.common.base.Objects;

@ReloadCodec(DictionaryValueCodec.class)
public class DictionaryValue implements DataValue {

	private final Key key;
	private final SingleValue value;

	DictionaryValue(Key key, SingleValue value) {
		this.key = key;
		this.value = value;
	}

	public Key getKey() {
		return key;
	}

	public SingleValue getValue() {
		return value;
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(super.hashCode(), key, value);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DictionaryValue other = (DictionaryValue) obj;
		if (key == null) {
			if (other.key != null)
				return false;
		} else if (!key.equals(other.key))
			return false;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "DictionaryValue [key=" + key + ", value=" + value + "]";
	}

	static public class Key {

		private final byte[] data;

		public Key(byte[] key) {
			if (key == null)
				throw new NullPointerException();
			data = key;
		}

		public byte[] getValue() {
			return data;
		}

		@Override
		public int hashCode() {
			return Objects.hashCode(super.hashCode(), data);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Key other = (Key) obj;
			if (!Arrays.equals(data, other.data))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return Codec.hexDump(data);
		}
	}

	static class KeyCodec extends Codec<Key> {

		private static final int KEY_LENGTH_FIELD = U_INT16;

		public KeyCodec(ComponentsContext ctx) {
			super(ctx);
		}

		@Override
		public void encode(Key obj, ByteBuf buf, Object... params) throws CodecException {
			Field lenFld = allocateField(buf, KEY_LENGTH_FIELD);
			buf.writeBytes(obj.data);
			lenFld.updateDataLength();
		}

		@Override
		public Key decode(ByteBuf buf, Object... params) throws CodecException {
			ByteBuf keyFld = readField(buf, KEY_LENGTH_FIELD);
			byte[] keyData = new byte[keyFld.readableBytes()];
			keyFld.readBytes(keyData);
			keyFld.release();
			return new Key(keyData);
		}

	}

	static class DictionaryValueCodec extends Codec<DictionaryValue> {

		private final Codec<SingleValue> valueCodec;
		private final Codec<Key> keyCodec;

		public DictionaryValueCodec(ComponentsContext ctx) {
			super(ctx);
			valueCodec = getCodec(SingleValue.class);
			keyCodec = getCodec(Key.class);
		}

		@Override
		public void encode(DictionaryValue obj, ByteBuf buf, Object... params) throws CodecException {
			keyCodec.encode(obj.key, buf);
			valueCodec.encode(obj.value, buf);
		}

		@Override
		public DictionaryValue decode(ByteBuf buf, Object... params) throws CodecException {
			Key k = keyCodec.decode(buf);
			SingleValue v = valueCodec.decode(buf);
			return new DictionaryValue(k, v);
		}

	}

	@Override
	public long getSize() {
		return value.getSize();
	}

}