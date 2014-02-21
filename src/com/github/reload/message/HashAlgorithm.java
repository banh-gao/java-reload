package com.github.reload.message;

import java.util.EnumSet;

/**
 * Defines the hash algorithms encoded as in RFC5246
 * 
 * @author Daniel Zozin <zdenial@gmx.com>
 * 
 */
public enum HashAlgorithm {
	NONE((byte) 0x00, "NONE"),
	MD5((byte) 0x01, "MD5"),
	SHA1((byte) 0x02, "SHA1"),
	SHA224((byte) 0x03, "SHA-224"),
	SHA256((byte) 0x04, "SHA-256"),
	SHA384((byte) 0x05, "SHA-384"),
	SHA512((byte) 0x06, "SHA-512");

	private final byte code;
	private final String asString;

	private HashAlgorithm(byte code, String asString) {
		this.code = code;
		this.asString = asString;
	}

	public byte getCode() {
		return code;
	}

	public static HashAlgorithm valueOf(short code) {
		for (HashAlgorithm a : EnumSet.allOf(HashAlgorithm.class))
			if (a.code == code)
				return a;
		return null;
	}

	public static HashAlgorithm getFromString(String v) {
		for (HashAlgorithm a : EnumSet.allOf(HashAlgorithm.class))
			if (a.asString.equalsIgnoreCase(v))
				return a;
		return null;
	}

	@Override
	/**
	 * Get hash algorithm string representation
	 */
	public String toString() {
		return asString;
	}
}