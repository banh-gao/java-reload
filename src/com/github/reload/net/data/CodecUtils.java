package com.github.reload.net.data;

import io.netty.buffer.ByteBuf;

/**
 * Utility class to write and read variable-length fields on byte buffers.
 * The data-length subfield uses only the needed bytes to represent the
 * maximum value for the data subfield length as an unsigned integer.
 */
public class CodecUtils {

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
	 *            the data maximum size in bytes
	 */
	public static int allocateField(ByteBuf buf, int maxDataLength) {
		int fieldPos = buf.writerIndex();
		buf.writerIndex(fieldPos + maxDataLength);
		return fieldPos;
	}

	/**
	 * Set data length to the length subfield at the given field position.
	 * The data length is calculated starting from the end of the length
	 * subfield up to the current buffer write index
	 * 
	 * @param buf
	 *            the buffer
	 * @param fieldPos
	 *            the start position of the field
	 * @param maxDataLength
	 *            the data maximum size in bytes
	 * 
	 * @return
	 *         the length of data subfield in bytes
	 * 
	 * @throws IndexOutOfBoundsException
	 *             if the written bytes after the length subfield exceeds the
	 *             data maximum length
	 */
	public static int setDataLength(ByteBuf buf, int fieldPos, int maxDataLength) {
		// current position - (start of data subfield)
		int writtenDataLength = buf.writerIndex() - (fieldPos + maxDataLength);

		if (writtenDataLength > maxDataLength)
			throw new IndexOutOfBoundsException("Trying to write " + writtenDataLength + " bytes where the maximum field length is " + maxDataLength + " bytes");

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
		int neededBytes = getNeededBytes(maxDataLength);
		for (int i = 0; i < neededBytes; i++) {
			int offset = (neededBytes - 1 - i);
			buf.writeByte(dataLength >>> offset);
		}
	}

	/**
	 * The mininum amount of bytes needed to represent the given value as an
	 * unsigned integer
	 */
	private static int getNeededBytes(int value) {
		int s = 1;
		while (s < 8 && value >= (1L << (s * 8)))
			s++;
		return s;
	}

	/**
	 * Read the length of a variable-length data field at the current read index
	 * for a field that can hold at most the data of the given amount of bytes.
	 * 
	 * @param buf
	 *            the buffer
	 * @param maxDataLength
	 *            the data maximum size in bytes
	 */
	public static int readDataLength(ByteBuf buf, int maxDataLength) {
		int neededBytes = getNeededBytes(maxDataLength);
		int decoded = 0;
		int baseOffset = (neededBytes - 1) * 8;
		for (int i = 0; i < neededBytes; i++) {
			int offset = baseOffset - (i * 8);
			decoded += (buf.readByte() & 0xff) << offset;
		}
		return decoded;
	}
}
