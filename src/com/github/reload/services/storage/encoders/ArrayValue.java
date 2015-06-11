package com.github.reload.services.storage.encoders;

import io.netty.buffer.ByteBuf;
import com.github.reload.components.ComponentsContext;
import com.github.reload.net.encoders.Codec;
import com.github.reload.net.encoders.Codec.ReloadCodec;
import com.github.reload.services.storage.encoders.ArrayModel.ArrayValueSpecifier;
import com.github.reload.services.storage.encoders.ArrayValue.ArrayEntryCodec;
import com.github.reload.services.storage.encoders.DataModel.DataValue;
import com.github.reload.services.storage.encoders.DataModel.ValueSpecifier;
import com.google.common.base.Objects;

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

	@Override
	public int hashCode() {
		return Objects.hashCode(super.hashCode(), index, value);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ArrayValue other = (ArrayValue) obj;
		if (index != other.index)
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
		return "ArrayValue [index=" + index + ", value=" + value + "]";
	}

	static class ArrayEntryCodec extends Codec<ArrayValue> {

		private final Codec<SingleValue> valueCodec;

		public ArrayEntryCodec(ComponentsContext ctx) {
			super(ctx);
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

	@Override
	public long getSize() {
		return value.getSize();
	}

	@Override
	public ValueSpecifier getMatchingSpecifier() {
		return new ArrayValueSpecifier().addRange(index, index);
	}
}