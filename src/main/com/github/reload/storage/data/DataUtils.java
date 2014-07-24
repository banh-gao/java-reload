package com.github.reload.storage.data;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.SignatureException;
import java.security.cert.Certificate;
import com.github.reload.conf.Configuration;
import com.github.reload.net.encoders.content.storage.ArrayModel;
import com.github.reload.net.encoders.content.storage.ArrayValue;
import com.github.reload.net.encoders.content.storage.SingleValue;
import com.github.reload.net.encoders.content.storage.StoredData;
import com.github.reload.net.encoders.content.storage.StoredMetadata;
import com.github.reload.net.encoders.header.ResourceID;
import com.github.reload.net.encoders.secBlock.Signature;
import com.github.reload.net.encoders.secBlock.SignerIdentity.IdentityType;
import com.github.reload.storage.DataKind;
import com.github.reload.storage.DataModel.Metadata;
import com.github.reload.storage.PreparedData.DataBuildingException;
import com.github.reload.storage.errors.DataTooLargeException;

public class DataUtils {

	/**
	 * Update the datakind of this data
	 * 
	 * @throws StorageException
	 *             if this stored data is not acceptable for the specified data
	 *             kind
	 */
	void updateDataKind(ResourceID resId, DataKind updatedKInd, Configuration conf) throws StorageException {
		performKindChecks(resId, this, updatedKInd, context);
		kind = updatedKInd;
	}

	/**
	 * Check if the stored data object is valid for the specified data kind
	 * 
	 * @throws StorageException
	 *             if the stored data is not valid
	 */
	static void performKindChecks(ResourceID resourceId, StoredData requestData, DataKind kind, Configuration conf) throws StorageException {
		kind.getAccessPolicy().accept(resourceId, requestData, requestData.getSignerIdentity(), context);

		if (requestData.getValue().getSize() > kind.getLongAttribute(DataKind.ATTR_MAX_SIZE))
			throw new DataTooLargeException("Size of the data exceeds the maximum allowed size");
	}

	/**
	 * Syntetic data value used to indicate non existent data in particular
	 * situations
	 */
	static StoredData getNonExistentData(DataKind kind) {
		try {
			return new StoredData(kind, BigInteger.ZERO, 0, kind.newPreparedData().getValue().setExists(false).build(), Signature.EMPTY_SIGNATURE);
		} catch (DataBuildingException e) {
			throw new RuntimeException(e);
		}
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

		signature.update(signBuf.array(), 0, signBuf.position());

		if (!signature.verify(null))
			throw new SignatureException("Invalid data signature");
	}

	StoredMetadata getMetadata() {
		return new StoredMetadata(kind, storageTime, lifeTime, value.getMetadata(), signature.getIdentity());
	}

	Metadata getMetadata() {
		return new Metadata(this);
	}
}
