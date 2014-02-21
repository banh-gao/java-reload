package com.github.reload.storage.data;

import io.netty.buffer.ByteBuf;
import java.math.BigInteger;
import com.github.reload.Context;
import com.github.reload.message.GenericSignature;
import com.github.reload.net.data.Codec;
import com.github.reload.net.data.ReloadCodec;
import com.github.reload.storage.StoredDataCodec;

/**
 * A stored data with a digital signature over the fields
 * 
 */
@ReloadCodec(StoredDataCodec.class)
public class StoredData {

	private final BigInteger storageTime;
	private final long lifeTime;
	private final DataValue value;
	private final GenericSignature signature;

	public StoredData(BigInteger storageTime, long lifeTime, DataValue value, GenericSignature signature) {
		this.storageTime = storageTime;
		this.lifeTime = lifeTime;
		this.value = value;
		this.signature = signature;
	}

	public BigInteger getStorageTime() {
		return storageTime;
	}

	public long getLifeTime() {
		return lifeTime;
	}

	public DataValue getValue() {
		return value;
	}

	GenericSignature getSignature() {
		return signature;
	}

	public static class StoredDataCodec extends Codec<StoredData> {

		private static final int DATA_LENGTH_FIELD = U_INT32;

		private final Codec<GenericSignature> signatureCodec;

		public StoredDataCodec(Context context) {
			super(context);
			signatureCodec = getCodec(GenericSignature.class);
		}

		@Override
		public StoredData decode(ByteBuf buf, Object... params) throws com.github.reload.net.data.Codec.CodecException {
			if (params.length < 1 || !(DataValue.class.isAssignableFrom(params[0].getClass())))
				throw new IllegalArgumentException("Data value class needed to decode a stored data");

			@SuppressWarnings("unchecked")
			Codec<DataValue> valueCodec = (Codec<DataValue>) getCodec(params[0].getClass());

			ByteBuf dataFld = readField(buf, DATA_LENGTH_FIELD);

			byte[] storageTimeRaw = new byte[8];
			dataFld.readBytes(storageTimeRaw);
			BigInteger storageTime = new BigInteger(1, storageTimeRaw);

			long lifeTime = dataFld.readUnsignedInt();

			DataValue value = valueCodec.decode(dataFld);

			GenericSignature signature = signatureCodec.decode(buf);

			return new StoredData(storageTime, lifeTime, value, signature);
		}

		@Override
		public void encode(StoredData obj, ByteBuf buf, Object... params) throws com.github.reload.net.data.Codec.CodecException {
			Field lenFld = allocateField(buf, DATA_LENGTH_FIELD);

			buf.writeBytes(obj.storageTime.toByteArray());
			buf.writeInt((int) obj.lifeTime);

			@SuppressWarnings("unchecked")
			Codec<DataValue> valueCodec = (Codec<DataValue>) getCodec(obj.value.getClass());

			valueCodec.encode(obj.value, buf);

			signatureCodec.encode(obj.signature, buf);

			lenFld.updateDataLength();
		}

	}
}
