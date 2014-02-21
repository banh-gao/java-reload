package com.github.reload.storage.data;

import java.math.BigInteger;
import com.github.reload.message.SignerIdentity;
import com.github.reload.storage.DataKind;
import com.github.reload.storage.Metadata;
import com.github.reload.storage.ResponseData;

/**
 * A data with a digital signature over the fields defined by the RELOAD
 * protocol
 * 
 * @author Daniel Zozin <zdenial@gmx.com>
 * 
 */
public class StoredMetadata extends ResponseData {

	private static final int METADATA_LENGTH_FIELD = EncUtils.U_INT32;

	public final static int DEFAULT_LIFETIME = 60;

	private final Metadata value;

	private final SignerIdentity signerIdentity;

	public StoredMetadata(DataKind kind, BigInteger storageTime, long lifetime, Metadata value, SignerIdentity signerIdentity) {
		this.kind = kind;
		this.storageTime = storageTime;
		lifeTime = lifetime;
		this.value = value;
		this.signerIdentity = signerIdentity;
	}

	public StoredMetadata(DataKind kind, UnsignedByteBuffer buf) {
		this.kind = kind;

		int length = buf.getLengthValue(METADATA_LENGTH_FIELD);

		storageTime = buf.getSigned64();
		lifeTime = buf.getSigned32();

		int metadataLength = length - EncUtils.U_INT64 - EncUtils.U_INT32;

		value = kind.parseMetadata(buf, metadataLength);
		signerIdentity = null;
	}

	@Override
	public void writeTo(UnsignedByteBuffer buf) {
		Field lenFld = buf.allocateLengthField(METADATA_LENGTH_FIELD);

		buf.putUnsigned64(storageTime);
		buf.putUnsigned32(lifeTime);
		value.writeTo(buf);

		lenFld.setEncodedLength(buf.getConsumedFrom(lenFld.getNextPosition()));
	}

	DataKind getKind() {
		return kind;
	}

	/**
	 * @return the timestamp storage time in milliseconds
	 */
	public BigInteger getStorageTime() {
		return storageTime;
	}

	/**
	 * @return the lifetime of this data in seconds
	 */
	public long getLifeTime() {
		return lifeTime;
	}

	public Metadata getMetadata() {
		return value;
	}

	@Override
	protected SignerIdentity getSignerIdentity() {
		return signerIdentity;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((kind == null) ? 0 : kind.hashCode());
		result = prime * result + ((storageTime == null) ? 0 : storageTime.hashCode());
		result = prime * result + ((value == null) ? 0 : value.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		StoredMetadata other = (StoredMetadata) obj;
		if (kind == null) {
			if (other.kind != null)
				return false;
		} else if (!kind.equals(other.kind))
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
		return "StoredMetadata [kind=" + kind.getKindId() + ", storageTime=" + storageTime + ", lifeTime=" + lifeTime + ", value=" + value + "]";
	}
}
