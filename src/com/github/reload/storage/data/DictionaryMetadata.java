package com.github.reload.storage.data;

import java.math.BigInteger;
import com.github.reload.storage.data.DictionaryValue.Key;

/**
 * Metadata of a stored dictionary entry
 * 
 */
public class DictionaryMetadata extends Metadata {

	Key key;

	public DictionaryMetadata(DictionaryValue value) {
		super(value);
		key = value.getKey();
	}

	public DictionaryMetadata(UnsignedByteBuffer buf) {
		super(buf);
	}

	@Override
	protected void implInitBefore(UnsignedByteBuffer buf) {
		key = new DictionaryValue.Key(buf);
	}

	public Key getKey() {
		return key;
	}

	@Override
	protected void implWriteToBefore(UnsignedByteBuffer buf) {
		key.writeTo(buf);
	}

	@Override
	public String toString() {
		return "DictionaryMetadata [key=" + new BigInteger(key.getValue()).toString(16) + ", exists=" + exists() + ", storedValueSize=" + getStoredValueSize() + ", hashAlgorithm=" + getHashAlgorithm() + ", hashValue=" + getHashValue() + "]";
	}
}
