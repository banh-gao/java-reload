package com.github.reload.services.storage.net;

import io.netty.buffer.ByteBuf;
import java.util.Arrays;
import com.github.reload.components.ComponentsContext;
import com.github.reload.net.encoders.Codec;
import com.github.reload.net.encoders.Codec.ReloadCodec;
import com.github.reload.services.storage.DataModel.DataValue;
import com.github.reload.services.storage.DataModel.ValueSpecifier;
import com.github.reload.services.storage.net.SingleValue.SingleEntryCodec;
import com.google.common.base.Objects;

@ReloadCodec(SingleEntryCodec.class)
public class SingleValue implements DataValue {

	private boolean exists;
	private byte[] value;

	public SingleValue(byte[] value, boolean exists) {
		this.value = value;
		this.exists = exists;
	}

	public void setValue(byte[] value) {
		this.value = value;
	}

	public void setExists(boolean exists) {
		this.exists = exists;
	}

	public byte[] getValue() {
		return value;
	}

	/**
	 * @return true if the value exists but it can be empty
	 */
	public boolean exists() {
		return exists;
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(super.hashCode(), exists, value);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SingleValue other = (SingleValue) obj;
		if (exists != other.exists)
			return false;
		if (!Arrays.equals(value, other.value))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "SingleValue [valueLength=" + value.length + ", exists=" + exists + "]";
	}

	static class SingleEntryCodec extends Codec<SingleValue> {

		final static int VALUE_LENGTH_FIELD = U_INT32;

		public SingleEntryCodec(ComponentsContext ctx) {
			super(ctx);
		}

		@Override
		public void encode(SingleValue obj, ByteBuf buf, Object... params) throws CodecException {
			buf.writeByte(obj.exists ? 1 : 0);
			Field lenFld = allocateField(buf, VALUE_LENGTH_FIELD);
			buf.writeBytes(obj.value);
			lenFld.updateDataLength();
		}

		@Override
		public SingleValue decode(ByteBuf buf, Object... params) throws CodecException {
			boolean exists = (buf.readUnsignedByte() >= 1);

			ByteBuf dataFld = readField(buf, VALUE_LENGTH_FIELD);

			byte[] value = new byte[dataFld.readableBytes()];
			dataFld.readBytes(value);
			dataFld.release();

			return new SingleValue(value, exists);

		}

	}

	@Override
	public long getSize() {
		return value.length;
	}

	@Override
	public ValueSpecifier getMatchingSpecifier() {
		return new SingleValueSpecifier();
	}
}