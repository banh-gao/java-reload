package com.github.reload.net.data;

import io.netty.buffer.ByteBuf;

/**
 * Utility class to write and read variable-length fields on byte buffers.
 * The data-length subfield uses only the needed bytes to represent the
 * maximum value for the data subfield length as an unsigned integer.
 */
public class CodecUtils {

	/**
	 * The amount of bytes needed to represent an unsigned integer up to
	 * 2<sup>8</sup>-1
	 */
	public static final int U_INT8 = 1;
	/**
	 * The amount of bytes needed to represent an unsigned integer up to
	 * 2<sup>16</sup>-1
	 */
	public static final int U_INT16 = 2;
	/**
	 * The amount of bytes needed to represent an unsigned integer up to
	 * 2<sup>24</sup>-1
	 */
	public static final int U_INT24 = 3;
	/**
	 * The amount of bytes needed to represent an unsigned integer up to
	 * 2<sup>32</sup>-1
	 */
	public static final int U_INT32 = 4;
	/**
	 * The amount of bytes needed to represent an unsigned integer up to
	 * 2<sup>64</sup>-1
	 */
	public static final int U_INT64 = 8;

	private CodecUtils() {
	}

	/**
	 * Allocate a field at the current write index that can hold at most the
	 * data of the given amount of bytes.
	 * This method is meant to be used with
	 * {@link #setVariableLengthField(ByteBuf, int, int)} to set the length
	 * subfield after the data have been written to the buffer
	 * 
	 * @param buf
	 *            the buffer
	 * @param maxDataLength
	 *            the data maximum bytes size in power of two
	 */
	public static Field allocateField(ByteBuf buf, int maxDataLength) {
		return new Field(buf, maxDataLength);
	}

	/**
	 * Returns the data stored in a variable-length field at the current read
	 * index.
	 * The returned buffer is a {@link ByteBuf#slice()} of the original buffer,
	 * it remains backed to the original buffer.
	 * 
	 * @param buf
	 *            the buffer
	 * @param maxDataLength
	 *            the data maximum bytes size in power of two
	 * 
	 * @see {@link ByteBuf#slice()}
	 */
	public static ByteBuf readDataLength(ByteBuf buf, int maxDataLength) {
		int dataLength = 0;
		int baseOffset = (maxDataLength - 1) * 8;
		for (int i = 0; i < maxDataLength; i++) {
			int offset = baseOffset - (i * 8);
			dataLength += (buf.readByte() & 0xff) << offset;
		}

		return buf.slice(buf.readerIndex(), dataLength);
	}

	public static class Field {

		private final ByteBuf buf;
		private final int fieldPos;
		private final int maxDataLength;

		public Field(ByteBuf buf, int maxDataLength) {
			this.buf = buf;
			fieldPos = buf.writerIndex();
			this.maxDataLength = maxDataLength;
			buf.writerIndex(fieldPos + maxDataLength);
		}

		/**
		 * Set data length of the field, its calculated starting from the end of
		 * the length subfield up to the current buffer write index
		 * 
		 * @return
		 *         the length of data subfield in bytes
		 */
		public int updateDataLength() {
			// current position - (start of data subfield)
			int writtenDataLength = buf.writerIndex() - (fieldPos + maxDataLength);
			// Set actual written data subfield length into the length subfield
			buf.writerIndex(fieldPos);
			encodeLength(maxDataLength, writtenDataLength, buf);

			// Reset 1 byte after original position (after the field)
			buf.writerIndex(buf.writerIndex() + writtenDataLength + 1);

			return writtenDataLength;
		}

		/**
		 * Encode length subfield
		 */
		private static void encodeLength(int maxDataLength, int dataLength, ByteBuf buf) {
			for (int i = 0; i < maxDataLength; i++) {
				int offset = (maxDataLength - 1 - i);
				buf.writeByte(dataLength >>> offset);
			}
		}

	}
}
