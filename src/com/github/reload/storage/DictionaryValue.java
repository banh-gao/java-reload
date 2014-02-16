package com.github.reload.storage;

import java.util.Arrays;

/**
 * A dictionary value represented by adding a key to a single value
 * 
 * @author Daniel Zozin <zdenial@gmx.com>
 * 
 */
public class DictionaryValue extends DataValue {

	private Key key;

	DictionaryValue(byte[] key, byte[] value, boolean exists) {
		super(value, exists);
		this.key = new Key(key);
	}

	public DictionaryValue(UnsignedByteBuffer buf) {
		super(buf);
	}

	@Override
	protected void implInitBefore(UnsignedByteBuffer buf) {
		key = new Key(buf);
	}

	public Key getKey() {
		return key;
	}

	@Override
	protected void implWriteBefore(UnsignedByteBuffer buf) {
		key.writeTo(buf);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((key == null) ? 0 : key.hashCode());
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
		DictionaryValue other = (DictionaryValue) obj;
		if (key == null) {
			if (other.key != null)
				return false;
		} else if (!key.equals(other.key))
			return false;
		return super.equals(obj);
	}

	static public class Key {

		public static final int KEY_LENGTH_FIELD = EncUtils.U_INT16;

		private final byte[] key;

		public Key(byte[] key) {
			if (key == null)
				throw new NullPointerException();
			this.key = key;
		}

		public Key(UnsignedByteBuffer encIn) {
			int len = encIn.getLengthValue(KEY_LENGTH_FIELD);
			key = new byte[len];
			encIn.getRaw(key);
		}

		public void writeTo(UnsignedByteBuffer buf) {
			Field lenFld = buf.allocateLengthField(Key.KEY_LENGTH_FIELD);
			buf.putRaw(key);
			lenFld.setEncodedLength(buf.getConsumedFrom(lenFld.getNextPosition()));
		}

		public byte[] getValue() {
			return key;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + Arrays.hashCode(key);
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
			Key other = (Key) obj;
			if (!Arrays.equals(key, other.key))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return EncUtils.toHexString(key);
		}
	}

	@Override
	public String toString() {
		return "DictionaryValue [key=" + key + ", exists=" + exists() + ", valueLength=" + getValue().length + "]";
	}
}
