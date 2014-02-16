package com.github.reload.storage;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.util.Date;
import com.github.reload.message.ResourceID;
import com.github.reload.storage.DataResponse.ResponseData;
import com.github.reload.storage.PreparedData.DataBuildingException;

/**
 * A data with a digital signature over the fields defined by the RELOAD
 * protocol
 * 
 * @author Daniel Zozin <zdenial@gmx.com>
 * 
 */
public class StoredData extends ResponseData {

	private static final int DATA_LENGTH_FIELD = EncUtils.U_INT32;

	private DataKind kind;
	private final BigInteger storageTime;
	private final long lifeTime;
	private final DataValue value;
	private final GenericSignature signature;

	/**
	 * Update the datakind of this data
	 * 
	 * @throws StorageException
	 *             if this stored data is not acceptable for the specified data
	 *             kind
	 */
	void updateDataKind(ResourceID resId, DataKind updatedKInd, Context context) throws StorageException {
		performKindChecks(resId, this, updatedKInd, context);
		kind = updatedKInd;
	}

	/**
	 * Check if the stored data object is valid for the specified data kind
	 * 
	 * @throws StorageException
	 *             if the stored data is not valid
	 */
	static void performKindChecks(ResourceID resourceId, StoredData requestData, DataKind kind, Context context) throws StorageException {
		kind.getAccessPolicy().accept(resourceId, requestData, requestData.getSignerIdentity(), context);

		if (requestData.getValue().getSize() > kind.getLongAttribute(DataKind.ATTR_MAX_SIZE))
			throw new DataTooLargeException("Size of the data exceeds the maximum allowed size");
	}

	public StoredData(DataKind kind, BigInteger storageTime, long lifetime, DataValue value, GenericSignature signature) {
		this.kind = kind;
		this.storageTime = storageTime;
		lifeTime = lifetime;
		this.value = value;
		this.signature = signature;
	}

	/**
	 * Syntetic data value used to indicate non existent data in paricular
	 * situations
	 */
	static StoredData getNonExistentData(DataKind kind) {
		try {
			return new StoredData(kind, BigInteger.ZERO, 0, kind.newPreparedData().getValue().setExists(false).build(), GenericSignature.EMPTY_SIGNATURE);
		} catch (DataBuildingException e) {
			throw new RuntimeException(e);
		}
	}

	public StoredData(DataKind kind, UnsignedByteBuffer buf) {
		this.kind = kind;

		int length = buf.getLengthValue(DATA_LENGTH_FIELD);

		storageTime = buf.getSigned64();
		lifeTime = buf.getSigned32();

		int valueLength = length - EncUtils.U_INT64 - EncUtils.U_INT32;

		value = kind.parseValue(buf, valueLength);

		try {
			signature = GenericSignature.parse(buf);
		} catch (NoSuchAlgorithmException e) {
			throw new DecodingException("Unsupported algorithm: " + e.getMessage());
		}
	}

	@Override
	protected void writeTo(UnsignedByteBuffer buf) {
		Field lenFld = buf.allocateLengthField(DATA_LENGTH_FIELD);

		buf.putUnsigned64(storageTime);
		buf.putUnsigned32(lifeTime);

		value.writeTo(buf);
		signature.writeTo(buf);

		lenFld.setEncodedLength(buf.getConsumedFrom(lenFld.getNextPosition()));
	}

	public DataKind getKind() {
		return kind;
	}

	/**
	 * @return the time when this data was stored
	 */
	public Date getStorageTime() {
		return new Date(storageTime.longValue());
	}

	/**
	 * @return the lifetime of this data in seconds
	 */
	public long getLifeTime() {
		return lifeTime;
	}

	public DataValue getValue() {
		return value;
	}

	GenericSignature getSignature() {
		return signature;
	}

	/**
	 * @return true if the signature over this data is valid
	 */
	void verify(Certificate signerCert, ResourceID resourceId) throws SignatureException, SignerIdentityException {
		if (signature.getIdentity().getIdentityType() == IdentityType.NONE)
			throw new SignerIdentityException("Forbitten identity type");

		try {
			signature.initVerify(signerCert.getPublicKey());
		} catch (InvalidKeyException e) {
			throw new SignatureException(e);
		}

		UnsignedByteBuffer signBuf = UnsignedByteBuffer.allocate(ResourceID.MAX_STRUCTURE_LENGTH + EncUtils.U_INT32 + EncUtils.U_INT64 + EncUtils.U_INT8 + DataValue.VALUE_LENGTH_FIELD + value.getSize());
		resourceId.writeTo(signBuf);
		kind.getKindId().writeTo(signBuf);
		signBuf.putUnsigned64(storageTime);

		// Avoid signature breaking for array
		if (value instanceof ArrayValue) {
			ArrayValue signValue = ArrayModel.getValueForSigning((ArrayValue) value, kind);
			signValue.writeTo(signBuf);
		} else {
			value.writeTo(signBuf);
		}

		signature.update(signBuf.array(), 0, signBuf.position());

		if (!signature.verify(null))
			throw new SignatureException("Invalid data signature");
	}

	StoredMetadata getMetadata() {
		return new StoredMetadata(kind, storageTime, lifeTime, value.getMetadata(), signature.getIdentity());
	}

	@Override
	protected SignerIdentity getSignerIdentity() {
		return signature.getIdentity();
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
		StoredData other = (StoredData) obj;
		if (kind == null) {
			if (other.kind != null)
				return false;
		} else if (!kind.equals(other.kind))
			return false;

		return value.equals(other.value);
	}

	@Override
	public String toString() {
		return "StoredData [kind=" + kind.getKindId() + ", storageTime=" + storageTime + ", lifeTime=" + lifeTime + ", value=" + value + ", signature=" + signature + "]";
	}
}
