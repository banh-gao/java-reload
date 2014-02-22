package com.github.reload.storage.data;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import com.github.reload.message.HashAlgorithm;

/**
 * Metadata used to describe a stored data
 * 
 */
public class Metadata {

	private static final int HASHVALUE_LENGTH_FIELD = EncUtils.U_INT8;

	private final boolean exists;
	private final long storedValueSize;
	private final HashAlgorithm hashAlgorithm;

	private final byte[] hashValue;

	public Metadata(SingleValue value) {
		exists = value.exists();
		storedValueSize = value.getSize();
		if (value.getSize() == 0) {
			hashAlgorithm = HashAlgorithm.NONE;
			hashValue = new byte[0];
		} else {
			hashAlgorithm = CryptoHelper.OVERLAY_HASHALG;
			hashValue = computeHash(hashAlgorithm, value);
		}
	}

	private static byte[] computeHash(HashAlgorithm hashAlgorithm, SingleValue value) {
		MessageDigest digestor;
		try {
			digestor = MessageDigest.getInstance(hashAlgorithm.toString());
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
		return digestor.digest(value.getValue());
	}

	public Metadata(UnsignedByteBuffer buf) {
		implInitBefore(buf);
		exists = (buf.getRaw8() != 0);
		storedValueSize = buf.getSigned32();
		hashAlgorithm = HashAlgorithm.valueOf(buf.getRaw8());

		int hashLength = buf.getLengthValue(HASHVALUE_LENGTH_FIELD);
		hashValue = new byte[hashLength];
		buf.getRaw(hashValue);
	}

	/**
	 * Called for subclasses before this metadata was init
	 * 
	 * @param buf
	 */
	protected void implInitBefore(UnsignedByteBuffer buf) {
		// No default operation
	}

	public boolean exists() {
		return exists;
	}

	public long getStoredValueSize() {
		return storedValueSize;
	}

	public HashAlgorithm getHashAlgorithm() {
		return hashAlgorithm;
	}

	public byte[] getHashValue() {
		return hashValue;
	}

	public final void writeTo(UnsignedByteBuffer buf) {
		implWriteToBefore(buf);
		buf.putRaw8((byte) (exists ? 1 : 0));
		buf.putUnsigned32(storedValueSize);
		buf.putRaw8(hashAlgorithm.getCode());
		Field lenFld = buf.allocateLengthField(HASHVALUE_LENGTH_FIELD);
		buf.putRaw(hashValue);
		lenFld.setEncodedLength(buf.getConsumedFrom(lenFld.getNextPosition()));
	}

	/**
	 * Called for subclasses before the superclass writes to buffer
	 * 
	 * @param buf
	 */
	protected void implWriteToBefore(UnsignedByteBuffer buf) {
		// No default operation
	}

	@Override
	public String toString() {
		return "Metadata [exists=" + exists + ", storedValueSize=" + storedValueSize + ", hashAlgorithm=" + hashAlgorithm + ", hashValue=" + hashValue + "]";
	}
}
