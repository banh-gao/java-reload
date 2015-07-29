package com.github.reload.services.storage.net;

import io.netty.buffer.ByteBuf;
import java.util.Arrays;
import com.github.reload.components.ComponentsContext;
import com.github.reload.net.encoders.Codec;
import com.github.reload.net.encoders.Codec.ReloadCodec;
import com.github.reload.services.storage.DataModel.DataValue;
import com.github.reload.services.storage.DataModel.ValueSpecifier;
import com.github.reload.services.storage.net.DictionaryValue.DictionaryValueCodec;
import com.google.common.base.Objects;

@ReloadCodec(DictionaryValueCodec.class)
public class DictionaryValue implements DataValue {

	private byte[] key;
	private SingleValue value = new SingleValue(new byte[0], true);

	public DictionaryValue() {
	}

	public DictionaryValue(byte[] key, SingleValue value) {
		this.key = key;
		this.value = value;
	}

	public void setKey(byte[] key) {
		this.key = key;
	}

	public void setValue(byte[] value, boolean exists) {
		this.value = new SingleValue(value, exists);
	}

	public byte[] getKey() {
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
		return "DictionaryValue [key=" + Arrays.toString(key) + ", value=" + value + "]";
	}

	static class DictionaryValueCodec extends Codec<DictionaryValue> {

		static final int KEY_LENGTH_FIELD = U_INT16;

		private final Codec<SingleValue> valueCodec;

		public DictionaryValueCodec(ComponentsContext ctx) {
			super(ctx);
			valueCodec = getCodec(SingleValue.class);
		}

		@Override
		public void encode(DictionaryValue obj, ByteBuf buf, Object... params) throws CodecException {
			encodeKey(obj.key, buf);
			valueCodec.encode(obj.value, buf);
		}

		static void encodeKey(byte[] key, ByteBuf buf) {
			Field lenFld = allocateField(buf, KEY_LENGTH_FIELD);
			buf.writeBytes(key);
			lenFld.updateDataLength();
		}

		@Override
		public DictionaryValue decode(ByteBuf buf, Object... params) throws CodecException {
			byte[] k = decodeKey(buf);
			SingleValue v = valueCodec.decode(buf);
			return new DictionaryValue(k, v);
		}

		static byte[] decodeKey(ByteBuf buf) {
			ByteBuf keyFld = readField(buf, KEY_LENGTH_FIELD);
			byte[] k = new byte[keyFld.readableBytes()];
			keyFld.readBytes(k);
			keyFld.release();
			return k;
		}

	}

	@Override
	public long getSize() {
		return value.getSize();
	}

	@Override
	public ValueSpecifier getMatchingSpecifier() {
		DictionaryValueSpecifier s = new DictionaryValueSpecifier();
		s.addKey(key);
		return s;
	}

}