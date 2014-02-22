package com.github.reload.storage.data;

import io.netty.buffer.ByteBuf;
import com.github.reload.Context;
import com.github.reload.net.data.Codec;
import com.github.reload.net.data.ReloadCodec;
import com.github.reload.storage.data.DataModel.DataValue;
import com.github.reload.storage.data.DictionaryValue.DictionaryEntryCodec;

@ReloadCodec(DictionaryEntryCodec.class)
public class DictionaryValue implements DataValue {

	private Key key;
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

	static public class Key {

		private final byte[] data;

		public Key(byte[] key) {
			if (key == null)
				throw new NullPointerException();
			this.data = key;
		}

		public byte[] getValue() {
			return data;
		}

		@Override
		public String toString() {
			return Codec.hexDump(data);
		}
	}

	public static class DictionaryEntryCodec extends Codec<DictionaryValue> {

		private static final int KEY_LENGTH_FIELD = U_INT16;

		private final Codec<SingleValue> valueCodec;

		public DictionaryEntryCodec(Context context) {
			super(context);
			valueCodec = getCodec(SingleValue.class);
		}

		@Override
		public void encode(DictionaryValue obj, ByteBuf buf, Object... params) throws CodecException {
			encodeKey(obj, buf);
			valueCodec.encode(obj.value, buf);
		}

		private void encodeKey(DictionaryValue obj, ByteBuf buf) {
			Field lenFld = allocateField(buf, KEY_LENGTH_FIELD);
			buf.writeBytes(obj.key.data);
			lenFld.updateDataLength();
		}

		@Override
		public DictionaryValue decode(ByteBuf buf, Object... params) throws CodecException {
			Key k = decodeKey(buf);
			SingleValue v = valueCodec.decode(buf);
			return new DictionaryValue(k, v);
		}

		private Key decodeKey(ByteBuf buf) {
			ByteBuf keyFld = readField(buf, KEY_LENGTH_FIELD);
			byte[] keyData = new byte[keyFld.readableBytes()];
			keyFld.readBytes(keyData);
			keyFld.release();
			return new Key(keyData);
		}

	}
}