package com.github.reload.services.storage.net;

import io.netty.buffer.ByteBuf;
import javax.inject.Inject;
import com.github.reload.components.ComponentsContext;
import com.github.reload.net.encoders.Codec;
import com.github.reload.net.encoders.Codec.ReloadCodec;
import com.github.reload.services.storage.DataModel.DataValue;
import com.github.reload.services.storage.DataModel.ValueSpecifier;
import com.github.reload.services.storage.net.ArrayValue.ArrayEntryCodec;
import com.google.common.base.Objects;

@ReloadCodec(ArrayEntryCodec.class)
public class ArrayValue implements DataValue {

	private static final long LAST_INDEX = 0xffffffffl;

	private long index = 0;
	private SingleValue value = new SingleValue();

	@Inject
	public ArrayValue() {
	}

	public ArrayValue(long index, SingleValue value) {
		this.index = index;
		this.value = value;
	}

	public void setIndex(long index) {
		this.index = index;
	}

	public void setAppend(boolean isAppend) {
		if (isAppend)
			index = LAST_INDEX;
	}

	public boolean isAppend() {
		return index == LAST_INDEX;
	}

	public void setValue(byte[] value, boolean exists) {
		this.value = new SingleValue(value, exists);
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
		ArrayValueSpecifier s = new ArrayValueSpecifier();
		s.addRange(index, index);
		return s;
	}
}