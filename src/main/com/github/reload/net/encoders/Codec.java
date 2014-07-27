package com.github.reload.net.encoders;

import io.netty.buffer.ByteBuf;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Constructor;
import java.math.BigInteger;
import java.util.Map;
import com.github.reload.conf.Configuration;
import com.github.reload.net.encoders.content.errors.ErrorRespose;
import com.github.reload.net.encoders.content.errors.ErrorType;
import com.google.common.collect.Maps;

/**
 * Encode and decode the object on the given buffer
 * 
 * @param <T>
 *            The object type handled by this codec
 */
public abstract class Codec<T> {

	/**
	 * Indicates the codec to be used for RELOAD encoding of the associated
	 * class.
	 * 
	 * @see Codec
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	public @interface ReloadCodec {

		Class<? extends Codec<?>> value();
	}

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

	protected final Configuration conf;

	protected final Map<Class<?>, Codec<?>> codecs = Maps.newHashMap();

	private static final Object[] NO_PARAMS = new Object[0];

	/**
	 * Construct new codec with the given configuration
	 * 
	 * @param conf
	 */
	public Codec(Configuration conf) {
		this.conf = conf;
	}

	/**
	 * Get an instance of the codec associated with the given class. The given
	 * class needs to specify its codec by using the {@link ReloadCodec}
	 * annotation.
	 * 
	 * @param clazz
	 *            the class for which the codec is requested
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <C> Codec<C> getCodec(Class<C> clazz) {
		Codec<?> codec = codecs.get(clazz);

		if (codec == null) {
			codec = getCodec(clazz, conf);
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
	 * The new codec will be initialized with the given {@link Configuration}.
	 * 
	 * @param clazz
	 *            the class that the codec is associated with
	 * @param conf
	 *            the configuration to be used to initialize to the codec or
	 *            null if the codec doesn't need one
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T> Codec<T> getCodec(Class<T> clazz, Configuration conf) {
		ReloadCodec codecAnn = clazz.getAnnotation(ReloadCodec.class);
		if (codecAnn == null)
			throw new IllegalStateException("No codec associated with " + clazz.toString());

		Class<? extends Codec<T>> codecClass = (Class<? extends Codec<T>>) codecAnn.value();

		try {
			Constructor<? extends Codec<?>> codecConstr = codecClass.getConstructor(Configuration.class);
			codecConstr.setAccessible(true);
			return (Codec<T>) codecConstr.newInstance(conf);
		} catch (Exception e) {
			e.printStackTrace();
			throw new IllegalStateException("Codec instantiation failed for " + clazz.toString(), e);
		}
	}

	/**
	 * Encode object to the given byte buffer
	 * 
	 * @param obj
	 * @param buf
	 */
	public final void encode(T obj, ByteBuf buf) throws CodecException {
		encode(obj, buf, NO_PARAMS);
	}

	/**
	 * Encode object to the given byte buffer. This method can be used to pass
	 * additional parameters to the encoder.
	 * 
	 * @param obj
	 * @param buf
	 * @param params
	 */
	public abstract void encode(T obj, ByteBuf buf, Object... params) throws CodecException;

	/**
	 * Decode object from the given byte buffer
	 * 
	 * @param buf
	 * @return
	 */
	public final T decode(ByteBuf buf) throws CodecException {
		return decode(buf, NO_PARAMS);
	}

	/**
	 * Decode object from the given byte buffer. This method can be used to pass
	 * additional parameters to the decoder.
	 * 
	 * @param buf
	 * @param params
	 * @return
	 */
	public abstract T decode(ByteBuf buf, Object... params) throws CodecException;

	/**
	 * Allocate a field at the current write index that can hold at most
	 * 2<sup>fldLenFactor</sup> bytes
	 * 
	 * @param buf
	 *            the buffer
	 * @param fldLenFactor
	 *            the data maximum bytes size in the powers of two
	 * 
	 * @see #readField(ByteBuf, int)
	 */
	public static Field allocateField(ByteBuf buf, int fldLenFactor) {
		return new Field(buf, fldLenFactor);
	}

	/**
	 * Returns the data stored in a variable-length field at the current read
	 * index and move the readIndex after the field.
	 * The returned buffer is a {@link ByteBuf#slice()} of the original buffer,
	 * it remains backed to the original buffer.
	 * The reference counter of the buffer is increase by 1 so remember to
	 * release the returned buffer after it is consumed in order to be recycled
	 * by the buffer allocator.
	 * 
	 * @param buf
	 *            the buffer
	 * @param fldLenFactor
	 *            the data maximum bytes size in the powers of two
	 * 
	 * @see {@link #allocateField(ByteBuf, int)}
	 */
	public static ByteBuf readField(ByteBuf buf, int fldLenFactor) {
		int dataLength = readLength(buf, fldLenFactor);

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

		public CodecException(Throwable cause) {
			super(cause);
		}

		@Override
		public ErrorType getErrorType() {
			return ErrorType.INVALID_MESSAGE;
		}

	}
}