package com.github.reload.storage.data;

import io.netty.buffer.ByteBuf;
import com.github.reload.Context;
import com.github.reload.net.data.Codec;
import com.github.reload.net.data.ReloadCodec;
import com.github.reload.storage.data.ArrayEntry.ArrayEntryCodec;

@ReloadCodec(ArrayEntryCodec.class)
public class ArrayEntry implements DataValue {

	private final long index;
	private final SingleEntry value;

	ArrayEntry(long index, SingleEntry value) {
		this.index = index;
		this.value = value;
	}

	public long getIndex() {
		return index;
	}

	public SingleEntry getValue() {
		return value;
	}

	public static class ArrayEntryCodec extends Codec<ArrayEntry> {

		private final Codec<SingleEntry> valueCodec;

		public ArrayEntryCodec(Context context) {
			super(context);
			valueCodec = getCodec(SingleEntry.class);
		}

		@Override
		public void encode(ArrayEntry obj, ByteBuf buf, Object... params) throws CodecException {
			buf.writeInt((int) obj.index);
			valueCodec.encode(obj.value, buf);
		}

		@Override
		public ArrayEntry decode(ByteBuf buf, Object... params) throws CodecException {
			long index = buf.readUnsignedInt();
			SingleEntry value = valueCodec.decode(buf);
			return new ArrayEntry(index, value);
		}

	}
}