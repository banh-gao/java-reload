package com.github.reload.net.encoders.content.storage;

import io.netty.buffer.ByteBuf;
import com.github.reload.Configuration;
import com.github.reload.net.encoders.Codec;
import com.github.reload.net.encoders.Codec.ReloadCodec;
import com.github.reload.net.encoders.content.storage.ArrayValue.ArrayEntryCodec;
import com.github.reload.storage.DataModel;
import com.github.reload.storage.DataModel.DataValue;

@ReloadCodec(ArrayEntryCodec.class)
public class ArrayValue implements DataValue {

	private final long index;
	private final SingleValue value;

	ArrayValue(long index, SingleValue value) {
		this.index = index;
		this.value = value;
	}

	public long getIndex() {
		return index;
	}

	public SingleValue getValue() {
		return value;
	}

	public static class ArrayEntryCodec extends Codec<ArrayValue> {

		private final Codec<SingleValue> valueCodec;

		public ArrayEntryCodec(Configuration conf) {
			super(conf);
			valueCodec = getCodec(SingleValue.class);
		}

		@Override
		public void encode(ArrayValue obj, ByteBuf buf, Object... params) throws CodecException {
			buf.writeInt((int) obj.index);
			valueCodec.encode(obj.value, buf);
		}

		@Override
		public ArrayValue decode(ByteBuf buf, Object... params) throws CodecException {
			long index = buf.readUnsignedInt();
			SingleValue value = valueCodec.decode(buf);
			return new ArrayValue(index, value);
		}

	}
}