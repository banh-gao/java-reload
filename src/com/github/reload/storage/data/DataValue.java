package com.github.reload.storage.data;

import java.util.Arrays;
import com.github.reload.storage.Metadata;

/**
 * The value of one data stored in the overlay
 * 
 */
public class DataValue {

	final static int VALUE_LENGTH_FIELD = EncUtils.U_INT32;

	private final boolean exists;
	private final byte[] value;

	public DataValue(byte[] value, boolean exists) {
		this.value = value;
		this.exists = exists;
	}

	public DataValue(UnsignedByteBuffer buf) {
		implInitBefore(buf);
		exists = (buf.getRaw8() >= 1);
		int len = buf.getLengthValue(VALUE_LENGTH_FIELD);

		value = new byte[len];
		buf.getRaw(value);
	}

	/**
	 * Called for subclasses before this value was init
	 * 
	 * @param buf
	 */
	protected void implInitBefore(UnsignedByteBuffer buf) {
		// No default operation
	}

	public byte[] getValue() {
		return value;
	}

	/**
	 * @return true if the value exists but it can be empty
	 */
	public boolean exists() {
		return exists;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (exists ? 1231 : 1237);
		result = prime * result + Arrays.hashCode(value);
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
		DataValue other = (DataValue) obj;
		if (exists != other.exists)
			return false;
		if (!Arrays.equals(value, other.value))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "SingleValue [valueLength=" + value.length + ", exists=" + exists + "]";
	}

	/**
	 * Write this value to the passed buffer
	 */
	final void writeTo(UnsignedByteBuffer buf) {
		implWriteBefore(buf);
		buf.putRaw8((byte) (exists ? 1 : 0));
		Field lenFld = buf.allocateLengthField(VALUE_LENGTH_FIELD);
		buf.putRaw(value);
		lenFld.setEncodedLength(buf.getConsumedFrom(lenFld.getNextPosition()));
	}

	/**
	 * @return the size of the value in bytes
	 */
	int getSize() {
		return value.length;
	}

	/**
	 * Called for subclasses before the superclass writes to buffer
	 * 
	 * @param buf
	 */
	protected void implWriteBefore(UnsignedByteBuffer buf) {
		// No default operation
	}

	Metadata getMetadata() {
		return new Metadata(this);
	}
}