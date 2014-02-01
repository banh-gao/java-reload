package com.github.reload.net.data;

import java.math.BigInteger;
import java.nio.ByteBuffer;

/**
 * A byte buffer used to read and write signed and unsigned values. It also
 * defines the field concept to rapidly read and write to a specific position
 * into the buffer.
 * 
 * @author Daniel Zozin <zdenial@gmx.com>
 * 
 */
public class UnsignedByteBuffer {

	/**
	 * The maximum value that can be represented with an unsigned 64 bits
	 * integer (equals to 2<sup>64</sup>-1)
	 */
	public static final BigInteger MAXVALUE_U_INT64 = BigInteger.valueOf(2).pow(64).subtract(BigInteger.ONE);

	public final ByteBuffer buf;

	private UnsignedByteBuffer(ByteBuffer buf) {
		this.buf = buf;
	}

	public static UnsignedByteBuffer allocate(int capacity) {
		return new UnsignedByteBuffer(ByteBuffer.allocate(capacity));
	}

	public static UnsignedByteBuffer wrap(byte[] array) {
		return new UnsignedByteBuffer(ByteBuffer.wrap(array));
	}

	public static UnsignedByteBuffer wrap(byte[] array, int offset, int length) {
		return new UnsignedByteBuffer(ByteBuffer.wrap(array, offset, length));
	}

	/**
	 * Write the given signed 8 bits value as an unsigned 8 bits value.
	 * The given value must be a positive value between 0 and 2<sup>8</sup>-1.
	 * 
	 * @param signed8
	 *            The value to be written, between 0 and 2<sup>8</sup>-1
	 * @return
	 */
	public UnsignedByteBuffer putUnsigned8(short signed8) {
		if (signed8 < 0 || CodecUtils.maxUnsignedInt(CodecUtils.U_INT8) < signed8)
			throw new IllegalArgumentException("Must be between 0 and 2^8-1");
		buf.put((byte) signed8);
		return this;
	}

	/**
	 * Write the given signed 16 bits value as an unsigned 16 bits value.
	 * The given value must be a positive value between 0 and 2<sup>16</sup>-1.
	 * 
	 * @param signed16
	 *            The value to be written, between 0 and 2<sup>16</sup>-1
	 * @return
	 */
	public UnsignedByteBuffer putUnsigned16(int signed16) {
		if (signed16 < 0 || CodecUtils.maxUnsignedInt(CodecUtils.U_INT16) < signed16)
			throw new IllegalArgumentException("Must be between 0 and 2^16-1");
		buf.put(ByteBuffer.allocate(2).putShort((short) signed16).array());
		return this;
	}

	/**
	 * Write the given signed 32 bits value as an unsigned 32 bits value.
	 * The given value must be a positive value between 0 and 2<sup>32</sup>-1.
	 * 
	 * @param signed32
	 *            The value to be written, between 0 and 2<sup>32</sup>-1
	 * @return
	 */
	public UnsignedByteBuffer putUnsigned32(long signed32) {
		if (signed32 < 0 || CodecUtils.maxUnsignedInt(CodecUtils.U_INT32) < signed32)
			throw new IllegalArgumentException("Must be between 0 and 2^32-1");
		buf.put(ByteBuffer.allocate(4).putInt((int) signed32).array());
		return this;
	}

	/**
	 * Write the given signed 64 bits value as an unsigned 64 bits value.
	 * The given value must be a positive value between 0 and 2<sup>64</sup>-1.
	 * 
	 * @param signed64
	 *            The value to be written, between 0 and 2<sup>64</sup>-1
	 * @return
	 */
	public UnsignedByteBuffer putUnsigned64(BigInteger signed64) {
		if (signed64.compareTo(BigInteger.ZERO) < 0 || MAXVALUE_U_INT64.compareTo(signed64) < 0)
			throw new IllegalArgumentException("Must be between 0 and 2^64-1");
		buf.put(ByteBuffer.allocate(8).putLong(signed64.longValue()).array());
		return this;
	}

	/**
	 * Write the given value directly into the buffer.
	 * 
	 * @param value
	 *            The value to be written
	 * @return
	 */
	public UnsignedByteBuffer putRaw8(byte value) {
		buf.put(value);
		return this;
	}

	/**
	 * Write the given value directly into the buffer.
	 * 
	 * @param value
	 *            The value to be written
	 * @return
	 */
	public UnsignedByteBuffer putRaw16(short value) {
		buf.putShort(value);
		return this;
	}

	/**
	 * Write the given value directly into the buffer.
	 * 
	 * @param value
	 *            The value to be written
	 * @return
	 */
	public UnsignedByteBuffer putRaw32(int value) {
		buf.putInt(value);
		return this;
	}

	/**
	 * Write the given value directly into the buffer.
	 * 
	 * @param value
	 *            The value to be written
	 * @return
	 */
	public UnsignedByteBuffer putRaw64(long value) {
		buf.putLong(value);
		return this;
	}

	/**
	 * Write the given value directly into the buffer.
	 * 
	 * @param src
	 * @return
	 */
	public UnsignedByteBuffer putRaw(byte[] src) {
		buf.put(src);
		return this;
	}

	/**
	 * Write the given value directly into the buffer, starting at the given
	 * <i>offset</i> for <i>len</i> bytes
	 * 
	 * @param src
	 * @return
	 */
	public UnsignedByteBuffer putRaw(byte[] src, int offset, int len) {
		buf.put(src, offset, len);
		return this;
	}

	/**
	 * Write all the remaining values from the given buffer
	 * 
	 * @param src
	 * @return
	 */
	public UnsignedByteBuffer putRaw(UnsignedByteBuffer src) {
		buf.put(src.buf);
		return this;
	}

	/**
	 * Reserve space at the current position for a length field of the specified
	 * size, the returned field is backed to this buffer.
	 * 
	 * @param fieldSize
	 *            the field size to be allocated in bytes
	 */
	public Field allocateLengthField(int fieldSize) {
		int fieldPos = position();
		position(fieldPos + fieldSize);
		return new Field(this, fieldPos, fieldSize);
	}

	/**
	 * Decode a length field at the current position
	 * 
	 * @return the length field value
	 */
	public int getLengthValue(int lengthFieldSize) {
		byte[] encLength = new byte[lengthFieldSize];
		getRaw(encLength, 0, lengthFieldSize);
		return decodeLength(encLength);
	}

	/**
	 * Get the length of the field using only the needed bytes to represent the
	 * maximum value for the field length. For example if we have a data length
	 * of 200 bytes then the length indicator needs 1 byte to represent the
	 * value 200. The field will be formatted as: |length indicator|data| . In
	 * this example the total length of the field will be 1 + 200, so the
	 * maximum field length must be at least of 201 bytes.
	 * 
	 * @param dataLength
	 *            the length of the data in bytes, the maximum size of the data
	 *            is maxFieldLength - lengthIndicatorLength
	 * @param lengthFieldSize
	 *            the maximum size of the length field including the length
	 *            indicator
	 *            space, must be great enough to contain both the
	 *            length_indicator + data
	 * @return the length of the data using only the amount of bytes needed to
	 *         represent the maximum field length
	 * @throws IllegalArgumentException
	 *             if the length indicator plus the data exceeds the maximum
	 *             field length.
	 */
	public static byte[] encodeLength(int dataLength, int lengthFieldSize) {
		if (dataLength < 0 || lengthFieldSize < 0)
			throw new IllegalArgumentException();

		long maxSize = CodecUtils.maxUnsignedInt(lengthFieldSize);

		if (lengthFieldSize + dataLength > maxSize)
			throw new IllegalArgumentException("Insufficient field length for encoded data, encoded data length will be " + (dataLength + lengthFieldSize) + " bytes but field maximum is " + maxSize + " bytes");

		byte[] out = new byte[lengthFieldSize];

		for (int i = 0; i < out.length; i++) {
			int offset = (out.length - 1 - i) * 8;
			out[i] = (byte) (dataLength >>> offset);
		}
		return out;
	}

	/**
	 * Decode a length value encoded with {@link #encodeLength(int, int)}
	 * 
	 * @param encLength
	 *            Encoded length value
	 * @return The decode length
	 */
	public static int decodeLength(byte[] encLength) {
		if (encLength.length < 0 || 4 < encLength.length)
			throw new IllegalArgumentException();

		int out = 0;
		int baseOffset = (encLength.length - 1) * 8;
		for (int i = 0; i < encLength.length; i++) {
			int offset = baseOffset - (i * 8);
			out += (encLength[i] & 0xff) << offset;
		}
		if (out < 0)
			throw new DecodingException("Illegal negative field length");

		return out;
	}

	/**
	 * @param startPos
	 *            the startPosition where to star counting
	 * @return the bytes consumed from the specified position
	 * @throws IllegalArgumentException
	 *             if startPos is greater than the current buffer position
	 */
	public int getConsumedFrom(int startPos) {
		if (startPos > position())
			throw new IllegalArgumentException();
		return position() - startPos;
	}

	public short getSigned8() {
		return (short) (getRaw8() & 0xff);
	}

	public int getSigned16() {
		return getRaw16() & 0xffff;
	}

	public long getSigned32() {
		return getRaw32() & 0xffffffffl;
	}

	public BigInteger getSigned64() {
		ByteBuffer b = ByteBuffer.allocate(8).putLong(getRaw64());
		b.rewind();
		return new BigInteger(1, b.array());

	}

	public byte getRaw8() {
		return buf.get();
	}

	public short getRaw16() {
		return buf.getShort();
	}

	public int getRaw32() {
		return buf.getInt();
	}

	public long getRaw64() {
		return buf.getLong();
	}

	public UnsignedByteBuffer getRaw(byte[] dst) {
		buf.get(dst);
		return this;
	}

	public UnsignedByteBuffer getRaw(byte[] dst, int offset, int length) {
		buf.get(dst, offset, length);
		return this;
	}

	public final int position() {
		return buf.position();
	}

	public final UnsignedByteBuffer position(int newPosition) {
		buf.position(newPosition);
		return this;
	}

	public final int remaining() {
		return buf.remaining();
	}

	public final UnsignedByteBuffer reset() {
		buf.reset();
		return this;
	}

	public final UnsignedByteBuffer rewind() {
		buf.rewind();
		return this;
	}

	public UnsignedByteBuffer slice() {
		return new UnsignedByteBuffer(buf.slice());
	}

	@Override
	public UnsignedByteBuffer clone() {
		int currentPos = buf.position();
		buf.rewind();
		ByteBuffer clone = ByteBuffer.allocate(buf.capacity());
		clone.put(buf);
		buf.position(currentPos);
		clone.flip();
		return new UnsignedByteBuffer(clone);
	}

	public String toString() {
		return buf.toString();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		UnsignedByteBuffer other = (UnsignedByteBuffer) obj;
		if (buf == null) {
			if (other.buf != null)
				return false;
		} else if (!buf.equals(other.buf))
			return false;
		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((buf == null) ? 0 : buf.hashCode());
		return result;
	}

	public ByteBuffer asByteBuffer() {
		return buf;
	}

	/**
	 * A delimited field the the associated buffer
	 */
	public static class Field {

		private final UnsignedByteBuffer buf;
		private int fieldPos;
		private int fieldSize;

		public Field(UnsignedByteBuffer buf, int pos, int size) {
			this.buf = buf;
			this.fieldPos = pos;
			this.fieldSize = size;
			if (this.fieldPos + this.fieldSize > buf.limit())
				throw new IllegalArgumentException("Field out of bounds");
		}

		/**
		 * Set the field value
		 * 
		 * @param value
		 */
		public void setValue(byte[] value) {
			int currentPos = buf.position();
			buf.position(fieldPos);
			buf.putRaw(value);
			buf.position(currentPos);
		}

		/**
		 * @return The field value
		 */
		public byte[] getValue() {
			int currentPos = buf.position();
			buf.position(fieldPos);
			byte[] value = new byte[fieldSize];
			buf.getRaw(value, 0, fieldSize);
			buf.position(currentPos);

			return value;
		}

		/**
		 * Set the field value as an encoded length
		 * 
		 * @param length
		 *            The length value
		 * @see #getDecodedLength()
		 */
		public void setEncodedLength(int length) {
			setValue(encodeLength(length, fieldSize));
		}

		/**
		 * @return Interpret the field value as an encoded length, returns the
		 *         decoded value
		 * @see #setEncodedLength(int)
		 */
		public int getDecodedLength() {
			return decodeLength(getValue());
		}

		/**
		 * @return The field position in the backed buffer
		 */
		public int getPosition() {
			return fieldPos;
		}

		/**
		 * @return The field size in bytes
		 */
		public int getSize() {
			return fieldSize;
		}

		/**
		 * @return The position in the buffer next to this field, equals to
		 *         {@link #getPosition()} + {@link #getSize()}
		 */
		public int getNextPosition() {
			return fieldPos + fieldSize;
		}

		/**
		 * @return The buffer this field is associated with
		 */
		public UnsignedByteBuffer getBuffer() {
			return buf;
		}
	}
}
