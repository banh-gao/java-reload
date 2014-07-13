package com.github.reload.storage.data;

import io.netty.buffer.ByteBuf;
import com.github.reload.Configuration;
import com.github.reload.message.Codec;
import com.github.reload.message.ReloadCodec;
import com.github.reload.storage.data.DataModel.DataValue;
import com.github.reload.storage.data.SingleValue.SingleEntryCodec;

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
	public String toString() {
		return "SingleValue [valueLength=" + value.length + ", exists=" + exists + "]";
	}

	public static class SingleEntryCodec extends Codec<SingleValue> {

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