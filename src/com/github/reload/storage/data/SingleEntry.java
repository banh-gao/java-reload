package com.github.reload.storage.data;

import io.netty.buffer.ByteBuf;
import com.github.reload.Context;
import com.github.reload.net.data.Codec;
import com.github.reload.net.data.ReloadCodec;
import com.github.reload.storage.data.SingleEntry.SingleValueCodec;

@ReloadCodec(SingleValueCodec.class)
public class SingleEntry {

	private final boolean exists;
	private final byte[] value;

	public SingleEntry(byte[] value, boolean exists) {
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

	public static class SingleValueCodec extends Codec<SingleEntry> {

		final static int VALUE_LENGTH_FIELD = U_INT32;

		public SingleValueCodec(Context context) {
			super(context);
		}

		@Override
		public void encode(SingleEntry obj, ByteBuf buf, Object... params) throws CodecException {
			buf.writeByte(obj.exists ? 1 : 0);
			Field lenFld = allocateField(buf, VALUE_LENGTH_FIELD);
			buf.writeBytes(obj.value);
			lenFld.updateDataLength();
		}

		@Override
		public SingleEntry decode(ByteBuf buf, Object... params) throws CodecException {
			boolean exists = (buf.readUnsignedByte() >= 1);

			ByteBuf dataFld = readField(buf, VALUE_LENGTH_FIELD);

			byte[] value = new byte[dataFld.readableBytes()];
			dataFld.readBytes(value);
			dataFld.release();

			return new SingleEntry(value, exists);

		}

	}
}