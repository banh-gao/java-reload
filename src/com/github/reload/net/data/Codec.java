package com.github.reload.net.data;

import io.netty.buffer.ByteBuf;
import java.lang.reflect.Constructor;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import com.github.reload.Context;
import com.github.reload.message.errors.ErrorRespose;
import com.github.reload.message.errors.ErrorType;

/**
 * Encode and decode the object on the given buffer
 * 
 * @param <T>
 *            The object type handled by this codec
 */
public abstract class Codec<T> {

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

	/**
	 * The amount of bytes needed to represent an unsigned integer up to
	 * 2<sup>128</sup>-1
	 */
	public static final int U_INT128 = 16;

	protected final Context context;

	protected final Map<Class<?>, Codec<?>> codecs = new HashMap<Class<?>, Codec<?>>();

	public Codec(Context context) {
		if (context == null)
			throw new NullPointerException();
		this.context = context;
	}

	/**
	 * Get an instance of the codec associated with the given class. The given
	 * class specify its codec class by the the {@link ReloadCodec} annotation.
	 * 
	 * @param clazz
	 *            the class that the codec is associated with
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <C> Codec<C> getCodec(Class<C> clazz) {
		Codec<?> codec = codecs.get(clazz);
		if (codec == null) {
			codec = getCodec(clazz, context);
			codecs.put(clazz, codec);
		}

		// Safe cast because the object is instantiated using reflection on the
		// class type given in input specific
		return (Codec<C>) codec;
	}

	/**
	 * Get an instance of the codec associated with the given class. The given
	 * class must be annotated with the {@link ReloadCodec} annotation to
	 * declare the codec class.
	 * The new codec will be initialized with the given {@link Context}.
	 * 
	 * @param clazz
	 *            the class that the codec is associated with
	 * @param ctx
	 *            the context used to initialize the codec
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T> Codec<T> getCodec(Class<T> clazz, Context context) {
		if (context == null)
			throw new NullPointerException();
		ReloadCodec codecAnn = clazz.getAnnotation(ReloadCodec.class);
		if (codecAnn == null)
			throw new IllegalStateException("No codec associated with " + clazz.toString());

		try {
			Constructor<? extends Codec<?>> codecConstr = codecAnn.value().getConstructor(Context.class);
			return (Codec<T>) codecConstr.newInstance(context);
		} catch (Exception e) {
			throw new IllegalStateException("Codec instantiation failed for " + clazz.toString(), e);
		}
	}

	/**
	 * Encode object to the given byte buffer
	 * 
	 * @param data
	 * @param buf
	 */
	public abstract void encode(T obj, ByteBuf buf) throws CodecException;

	/**
	 * Decode object from the given byte buffer
	 * 
	 * @param buf
	 * @return
	 */
	public abstract T decode(ByteBuf buf) throws CodecException;

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
	 * index and move the readIndex after the field.
	 * The returned buffer is a {@link ByteBuf#slice()} of the original buffer,
	 * it remains backed to the original buffer and increases the original
	 * buffer references count by 1.
	 * Remember to release the returned buffer after it is consumed.
	 * 
	 * @param buf
	 *            the buffer
	 * @param maxDataLength
	 *            the data maximum bytes size in power of two
	 * 
	 * @see {@link ByteBuf#slice()}
	 */
	public static ByteBuf readField(ByteBuf buf, int maxDataLength) {
		int dataLength = readLength(buf, maxDataLength);

		ByteBuf data = buf.readBytes(dataLength);
		data.retain();

		return data;
	}

	public static int readLength(ByteBuf buf, int maxDataLength) {
		int dataLength = 0;
		int baseOffset = (maxDataLength - 1) * 8;
		for (int i = 0; i < maxDataLength; i++) {
			int offset = baseOffset - (i * 8);
			dataLength += buf.readUnsignedByte() << offset;
		}
		return dataLength;
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
			buf.writerIndex(buf.writerIndex() + writtenDataLength);

			return writtenDataLength;
		}

		/**
		 * Encode length subfield
		 */
		private static void encodeLength(int maxDataLength, int dataLength, ByteBuf buf) {
			for (int i = 0; i < maxDataLength; i++) {
				int offset = (maxDataLength - 1 - i) * 8;
				buf.writeByte(dataLength >>> offset);
			}
		}
	}

	/**
	 * @return the hexadecimal string representation of the passed bytes
	 */
	public static String hexDump(byte[] bytes) {
		BigInteger bi = new BigInteger(1, bytes);
		return String.format("%#x", bi);
	}

	/**
	 * @return the hexadecimal string representation of the passed value without
	 *         sign
	 */
	public static String hexDump(long val) {
		BigInteger bi = BigInteger.valueOf(val).abs();
		return String.format("%#x", bi);
	}

	/**
	 * This exception can be thrown to indicate an error in the en/decoding
	 * process
	 */
	public static class CodecException extends Exception implements ErrorRespose {

		public CodecException(String message, Throwable cause) {
			super(message, cause);
		}

		public CodecException(String message) {
			super(message);
		}

		@Override
		public ErrorType getErrorType() {
			return ErrorType.INVALID_MESSAGE;
		}

	}
}