package com.github.reload.message;

import java.util.EnumSet;

/**
 * Defines the signature algorithms encoded as in RFC5246
 * 
 * @author Daniel Zozin <zdenial@gmx.com>
 * 
 */
public enum SignatureAlgorithm {
	ANONYMOUS((byte) 0x00, "ANONYMOUS"),
	RSA((byte) 0x01, "RSA"),
	DSA((byte) 0x02, "DSA"),
	ECDSA((byte) 0x03, "ECDSA");

	private final byte code;
	private final String asString;

	private SignatureAlgorithm(byte code, String asString) {
		this.code = code;
		this.asString = asString;
	}

	public byte getCode() {
		return code;
	}

	public static SignatureAlgorithm valueOf(byte code) {
		for (SignatureAlgorithm a : EnumSet.allOf(SignatureAlgorithm.class))
			if (a.code == code)
				return a;
		return null;
	}

	public static SignatureAlgorithm getFromString(String v) {
		for (SignatureAlgorithm a : EnumSet.allOf(SignatureAlgorithm.class))
			if (a.asString.equalsIgnoreCase(v))
				return a;
		return null;
	}

	@Override
	public String toString() {
		return asString;
	}
}