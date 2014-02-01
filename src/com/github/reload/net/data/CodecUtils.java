package com.github.reload.net.data;

import java.math.BigInteger;

/**
 * Utility class for on low level data encoding and decoding
 * 
 * @author Daniel Zozin <zdenial@gmx.com>
 * 
 */
public class CodecUtils {

	/**
	 * The number of bytes needed to represent a 8 bits unsigned integer
	 */
	public static final byte U_INT8 = 1;
	/**
	 * The number of bytes needed to represent a 16 bits unsigned integer
	 */
	public static final byte U_INT16 = 2;
	/**
	 * The number of bytes needed to represent a 24 bits unsigned integer
	 */
	public static final byte U_INT24 = 3;
	/**
	 * The number of bytes needed to represent a 32 bits unsigned integer
	 */
	public static final byte U_INT32 = 4;
	/**
	 * The number of bytes needed to represent a 64 bits unsigned integer
	 */
	public static final byte U_INT64 = 8;
	/**
	 * The number of bytes needed to represent a 128 bits unsigned integer
	 */
	public static final byte U_INT128 = 16;

	/**
	 * The maximum value that can be represented with an unsigned integer
	 * (values corresponds to 2<sup>index*8</sup>-1)
	 */
	private static final long[] MAX_VALUE = new long[U_INT128];

	static {
		for (int i = 0; i < MAX_VALUE.length; i++) {
			MAX_VALUE[i] = (long) Math.pow(2, i * 8) - 1;
		}
	}

	private CodecUtils() {
	}

	/**
	 * @param len
	 *            The number of bytes available
	 * @return The maximum unsigned int that can be represented with the given
	 *         amount of bytes
	 */
	public static long maxUnsignedInt(int len) {
		if (len > MAX_VALUE.length)
			return (long) Math.pow(2, len) - 1;
		else
			return MAX_VALUE[len];
	}

	/**
	 * @return the hexadecimal string representation of the passed bytes
	 */
	public static String toHexString(byte[] bytes) {
		BigInteger bi = new BigInteger(1, bytes);
		return String.format("%#x", bi);
	}

	/**
	 * @return the hexadecimal string representation of the passed value without
	 *         sign
	 */
	public static String toHexString(long val) {
		BigInteger bi = BigInteger.valueOf(val).abs();
		return String.format("%#x", bi);
	}
}
