package com.github.reload.storage;

import java.math.BigInteger;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.util.Date;
import com.github.reload.message.NodeID;
import com.github.reload.message.ResourceID;
import com.github.reload.storage.data.ArrayValue;
import com.github.reload.storage.data.ArrayModel;
import com.github.reload.storage.data.SingleValue;
import com.github.reload.storage.data.StoredData;

/**
 * Helps to generate a signed data
 * 
 * @author Daniel Zozin <zdenial@gmx.com>
 * 
 */
public class PreparedData {

	private final static int DEFAULT_LIFETIME = 60;

	private final ValueBuilder preparedValue;
	private final DataKind kind;

	private BigInteger generation = BigInteger.ZERO;
	private BigInteger storageTime = BigInteger.valueOf(new Date().getTime());
	private long lifeTime = DEFAULT_LIFETIME;

	PreparedData(DataKind kind, ValueBuilder preparedValue) {
		this.preparedValue = preparedValue;
		this.kind = kind;
	}

	/**
	 * @return the associated datakind
	 */
	public DataKind getKind() {
		return kind;
	}

	/**
	 * Set the date this data is stored, this is used in store checks for future
	 * stores on the same resource-id. If not specified, the current system time
	 * will be used.
	 */
	public PreparedData setStorageTime(Date storageTime) {
		this.storageTime = BigInteger.valueOf(storageTime.getTime());
		return this;
	}

	public Date getStorageTime() {
		return new Date(storageTime.longValue());
	}

	/**
	 * Set the number of seconds until this value should be deleted from the
	 * overlay.
	 */
	public PreparedData setLifeTime(long lifeTime) {
		this.lifeTime = lifeTime;
		return this;
	}

	/**
	 * @return The number of seconds until this value should be deleted from the
	 *         overlay.
	 */
	public long getLifeTime() {
		return lifeTime;
	}

	/**
	 * @param generation
	 *            The version number of this data into the overlay storage, if
	 *            the value is less than the currently stored value the store
	 *            request will be rejected
	 */
	public PreparedData setGeneration(BigInteger generation) {
		this.generation = generation;
		return this;
	}

	/**
	 * @return The version number of this data into the overlay storage
	 */
	public BigInteger getGeneration() {
		return generation;
	}

	/**
	 * @return the prepared value backed with this data, the runtime prepared
	 *         value type depends on the data model used by the associated data
	 *         kind
	 */
	public ValueBuilder getValue() {
		return preparedValue;
	}

	/**
	 * Creates the stored data object
	 * 
	 * @param context
	 *            The connection context in which the data will be used
	 * @param resourceId
	 *            The resource where the data will be stored. This is needed for
	 *            data signature computation
	 * 
	 * @return The stored data for the specified overlay
	 * @throws DataBuildingException
	 */
	StoredData build(Context context, ResourceID resourceId) throws DataBuildingException {

		CryptoHelper cryptoHelper = context.getCryptoHelper();

		SingleValue value = preparedValue.build();

		HashAlgorithm certHashAlg = cryptoHelper.getCertHashAlg();

		Certificate localCert = cryptoHelper.getLocalCertificate().getOriginalCertificate();

		NodeID storerId = context.getLocalId();

		// Use this identity type because it is required by some access policies
		SignerIdentity localIdentity = SignerIdentity.multipleIdIdentity(certHashAlg, localCert, storerId);

		GenericSignature signature = computeSignature(localIdentity, cryptoHelper, resourceId, value);

		return new StoredData(kind, storageTime, lifeTime, value, signature);
	}

	private GenericSignature computeSignature(SignerIdentity signerIdentity, CryptoHelper cryptoHelper, ResourceID resourceId, SingleValue value) {
		GenericSignature dataSigner = cryptoHelper.newSigner(signerIdentity);

		UnsignedByteBuffer signBuf = UnsignedByteBuffer.allocate(ResourceID.MAX_STRUCTURE_LENGTH + EncUtils.U_INT32 + EncUtils.U_INT64 + EncUtils.U_INT8 + SingleValue.VALUE_LENGTH_FIELD + value.getSize());

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

		try {
			dataSigner.update(signBuf.array(), 0, signBuf.position());
			dataSigner.sign();
			return dataSigner;
		} catch (SignatureException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Indicates an error occurred while building a storage data object
	 */
	public static class DataBuildingException extends RuntimeException {

		public DataBuildingException(String message) {
			super(message);
		}
	}
}