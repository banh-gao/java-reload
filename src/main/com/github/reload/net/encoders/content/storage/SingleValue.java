package com.github.reload.net.encoders.content.storage;

import io.netty.buffer.ByteBuf;
import java.util.Arrays;
import com.github.reload.Configuration;
import com.github.reload.net.encoders.Codec;
import com.github.reload.net.encoders.Codec.ReloadCodec;
import com.github.reload.net.encoders.content.storage.SingleValue.SingleEntryCodec;
import com.github.reload.storage.DataModel.DataValue;
import com.google.common.base.Objects;

@ReloadCodec(SingleEntryCodec.class)
public class SingleValue implements DataValue {

	private final boolean exists;
	private final byte[] value;

	public SingleValue(byte[] value, boolean exists) {
		this.value = value;
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

		public SingleEntryCodec(Configuration conf) {
			super(conf);
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

}