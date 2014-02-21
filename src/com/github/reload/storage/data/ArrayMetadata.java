package com.github.reload.storage.data;

import com.github.reload.storage.Metadata;


/**
 * Metadata of a stored array entry
 * 
 * @author Daniel Zozin <zdenial@gmx.com>
 * 
 */
public class ArrayMetadata extends Metadata {

	private long index;

	public ArrayMetadata(ArrayValue value) {
		super(value);
		index = value.getIndex();
	}

	public ArrayMetadata(UnsignedByteBuffer buf) {
		super(buf);
	}

	@Override
	protected void implInitBefore(UnsignedByteBuffer buf) {
		index = buf.getSigned32();
	}

	@Override
	protected void implWriteToBefore(UnsignedByteBuffer buf) {
		buf.putUnsigned32(index);
	}

	public long getIndex() {
		return index;
	}

	@Override
	public String toString() {
		return "ArrayMetadata [index=" + index + ", exists=" + exists() + ", storedValueSize=" + getStoredValueSize() + ", hashAlgorithm=" + getHashAlgorithm() + ", hashValue=" + getHashValue() + "]";
	}
}
