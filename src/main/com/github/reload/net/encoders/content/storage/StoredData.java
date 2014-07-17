package com.github.reload.net.encoders.content.storage;

import io.netty.buffer.ByteBuf;
import java.math.BigInteger;
import com.github.reload.Configuration;
import com.github.reload.net.encoders.Codec;
import com.github.reload.net.encoders.Codec.ReloadCodec;
import com.github.reload.net.encoders.content.storage.StoredData.StoredDataCodec;
import com.github.reload.net.encoders.secBlock.Signature;
import com.github.reload.storage.DataModel;
import com.github.reload.storage.DataModel.DataValue;

@ReloadCodec(StoredDataCodec.class)
public class StoredData {

	private final BigInteger storageTime;
	private final long lifeTime;
	private final DataValue value;
	private final Signature signature;

	public StoredData(BigInteger storageTime, long lifeTime, DataValue value, Signature signature) {
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

	Signature getSignature() {
		return signature;
	}

	public static class StoredDataCodec extends Codec<StoredData> {

		private static final int STORAGE_TIME_FIELD = U_INT64;
		private static final int DATA_LENGTH_FIELD = U_INT32;

		private final Codec<Signature> signatureCodec;

		public StoredDataCodec(Configuration conf) {
			super(conf);
			signatureCodec = getCodec(Signature.class);
		}

		@Override
		public void encode(StoredData obj, ByteBuf buf, Object... params) throws com.github.reload.net.encoders.Codec.CodecException {
			Field lenFld = allocateField(buf, DATA_LENGTH_FIELD);

			byte[] storageTimeBytes = obj.storageTime.toByteArray();

			// Make sure field is always of the fixed size by padding with zeros
			buf.writeZero(STORAGE_TIME_FIELD - storageTimeBytes.length);

			buf.writeBytes(storageTimeBytes);

			buf.writeInt((int) obj.lifeTime);

			@SuppressWarnings("unchecked")
			Codec<DataValue> valueCodec = (Codec<DataValue>) getCodec(obj.value.getClass());

			valueCodec.encode(obj.value, buf);

			signatureCodec.encode(obj.signature, buf);

			lenFld.updateDataLength();
		}

		@Override
		public StoredData decode(ByteBuf buf, Object... params) throws com.github.reload.net.encoders.Codec.CodecException {
			if (params.length < 1 || !(params[0] instanceof DataModel))
				throw new IllegalStateException("Data model needed to decode a stored data");

			ByteBuf dataFld = readField(buf, DATA_LENGTH_FIELD);

			byte[] storageTimeBytes = new byte[STORAGE_TIME_FIELD];
			dataFld.readBytes(storageTimeBytes);
			BigInteger storageTime = new BigInteger(1, storageTimeBytes);

			long lifeTime = dataFld.readUnsignedInt();

			@SuppressWarnings("unchecked")
			Codec<DataValue> valueCodec = (Codec<DataValue>) getCodec(((DataModel<?>) params[0]).getValueClass());

			DataValue value = valueCodec.decode(dataFld);

			Signature signature = signatureCodec.decode(dataFld);

			return new StoredData(storageTime, lifeTime, value, signature);
		}

	}
}
