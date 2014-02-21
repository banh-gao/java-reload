package com.github.reload.storage.data;

import io.netty.buffer.ByteBuf;
import com.github.reload.Context;
import com.github.reload.net.data.Codec;
import com.github.reload.net.data.ReloadCodec;
import com.github.reload.storage.data.DictionaryEntry.DictionaryEntryCodec;

@ReloadCodec(DictionaryEntryCodec.class)
public class DictionaryEntry implements DataValue {

	private Key key;
	private final SingleEntry value;

	DictionaryEntry(Key key, SingleEntry value) {
		this.key = key;
		this.value = value;
	}

	public Key getKey() {
		return key;
	}

	public SingleEntry getValue() {
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

	public static class DictionaryEntryCodec extends Codec<DictionaryEntry> {

		private static final int KEY_LENGTH_FIELD = U_INT16;

		private final Codec<SingleEntry> valueCodec;

		public DictionaryEntryCodec(Context context) {
			super(context);
			valueCodec = getCodec(SingleEntry.class);
		}

		@Override
		public void encode(DictionaryEntry obj, ByteBuf buf, Object... params) throws CodecException {
			encodeKey(obj, buf);
			valueCodec.encode(obj.value, buf);
		}

		private void encodeKey(DictionaryEntry obj, ByteBuf buf) {
			Field lenFld = allocateField(buf, KEY_LENGTH_FIELD);
			buf.writeBytes(obj.key.data);
			lenFld.updateDataLength();
		}

		@Override
		public DictionaryEntry decode(ByteBuf buf, Object... params) throws CodecException {
			Key k = decodeKey(buf);
			SingleEntry v = valueCodec.decode(buf);
			return new DictionaryEntry(k, v);
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