package com.github.reload.services.storage.encoders;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.UnpooledByteBufAllocator;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.security.SignatureException;
import java.util.Objects;
import com.github.reload.conf.Configuration;
import com.github.reload.crypto.Signer;
import com.github.reload.net.encoders.Codec;
import com.github.reload.net.encoders.Codec.CodecException;
import com.github.reload.net.encoders.Codec.ReloadCodec;
import com.github.reload.net.encoders.header.ResourceID;
import com.github.reload.net.encoders.secBlock.HashAlgorithm;
import com.github.reload.net.encoders.secBlock.Signature;
import com.github.reload.services.storage.DataKind;
import com.github.reload.services.storage.DataModel;
import com.github.reload.services.storage.DataModel.DataValue;
import com.github.reload.services.storage.encoders.StoredData.StoredDataCodec;

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

	public StoredData(BigInteger storageTime, long lifeTime, DataValue value, Signer s, ResourceID resId, DataKind kind) throws SignatureException {
		this(storageTime, lifeTime, value, generateSignature(storageTime, lifeTime, value, s, resId, kind));
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

	public StoredMetadata getMetadata(DataKind kind, HashAlgorithm hashAlg) {
		@SuppressWarnings("unchecked")
		DataModel<DataValue> m = (DataModel<DataValue>) kind.getDataModel();
		return new StoredMetadata(storageTime, lifeTime, m.newMetadata(value, hashAlg));
	}

	public Signature getSignature() {
		return signature;
	}

	private static Signature generateSignature(BigInteger storageTime, long lifeTime, DataValue value, Signer s, ResourceID resId, DataKind kind) throws SignatureException {
		ByteBuf b = UnpooledByteBufAllocator.DEFAULT.buffer();
		b.writeBytes(resId.getData());
		b.writeLong(kind.getKindId());
		b.writeBytes(storageTime.toByteArray());

		@SuppressWarnings("unchecked")
		Codec<DataValue> valueCodec = (Codec<DataValue>) Codec.getCodec(value.getClass(), null);
		try {
			valueCodec.encode(value, b);
			s.update(b);
		} catch (CodecException e) {
			throw new RuntimeException(e);
		} finally {
			b.release();
		}

		return s.sign();
	}

	public boolean verify(PublicKey publicKey, ResourceID resId, DataKind kind) throws GeneralSecurityException {
		ByteBuf b = UnpooledByteBufAllocator.DEFAULT.buffer();
		b.writeBytes(resId.getData());
		b.writeLong(kind.getKindId());
		b.writeBytes(storageTime.toByteArray());

		@SuppressWarnings("unchecked")
		Codec<DataValue> valueCodec = (Codec<DataValue>) Codec.getCodec(value.getClass(), null);
		try {
			valueCodec.encode(value, b);
		} catch (CodecException e) {
			throw new RuntimeException(e);
		}

		return signature.verify(b, publicKey);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), storageTime, lifeTime, value, signature);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		StoredData other = (StoredData) obj;
		if (lifeTime != other.lifeTime)
			return false;
		if (signature == null) {
			if (other.signature != null)
				return false;
		} else if (!signature.equals(other.signature))
			return false;
		if (storageTime == null) {
			if (other.storageTime != null)
				return false;
		} else if (!storageTime.equals(other.storageTime))
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
		return "StoredData [storageTime=" + storageTime + ", lifeTime=" + lifeTime + ", value=" + value + ", signature=" + signature + "]";
	}

	static class StoredDataCodec extends Codec<StoredData> {

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
