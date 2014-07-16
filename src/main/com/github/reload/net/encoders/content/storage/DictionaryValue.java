package com.github.reload.net.encoders.content.storage;

import io.netty.buffer.ByteBuf;
import com.github.reload.Configuration;
import com.github.reload.net.encoders.Codec;
import com.github.reload.net.encoders.Codec.ReloadCodec;
import com.github.reload.net.encoders.content.storage.DictionaryValue.DictionaryValueCodec;
import com.github.reload.storage.data.DataModel;
import com.github.reload.storage.data.DataModel.DataValue;

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
		public String toString() {
			return Codec.hexDump(data);
		}
	}

	public static class KeyCodec extends Codec<Key> {

		private static final int KEY_LENGTH_FIELD = U_INT16;

		public KeyCodec(Configuration conf) {
			super(conf);
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

	public static class DictionaryValueCodec extends Codec<DictionaryValue> {

		private final Codec<SingleValue> valueCodec;
		private final Codec<Key> keyCodec;

		public DictionaryValueCodec(Configuration conf) {
			super(conf);
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
}