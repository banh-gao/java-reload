package com.github.reload.storage;


/**
 * An array value represented by adding an index to a single value
 * 
 * @author Daniel Zozin <zdenial@gmx.com>
 * 
 */
public class ArrayValue extends DataValue {

	long index;

	ArrayValue(long index, byte[] value, boolean exists) {
		super(value, exists);
		this.index = index;
	}

	public ArrayValue(UnsignedByteBuffer buf) {
		super(buf);
	}

	@Override
	protected void implInitBefore(UnsignedByteBuffer buf) {
		index = buf.getSigned32();
	}

	public long getIndex() {
		return index;
	}

	@Override
	protected void implWriteBefore(UnsignedByteBuffer buf) {
		buf.putUnsigned32(index);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + (int) (index ^ (index >>> 32));
		return result + super.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		ArrayValue other = (ArrayValue) obj;
		if (index != other.index)
			return false;

		return super.equals(obj);
	}

	@Override
	public String toString() {
		return "ArrayValue [index=" + index + ", exists=" + exists() + ", valueLength=" + getValue().length + "]";
	}
}