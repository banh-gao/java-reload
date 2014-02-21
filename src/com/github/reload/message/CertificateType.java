package com.github.reload.message;

import java.util.EnumSet;

/**
 * The certificate type identifier of exchanced certificates
 * 
 * @author Daniel Zozin <zdenial@gmx.com>
 * 
 */
public enum CertificateType {
	X509((byte) 0, "X.509"), PGP((byte) 1, "openPGP");

	private final byte code;
	private final String type;

	private CertificateType(byte code, String type) {
		this.code = code;
		this.type = type;
	}

	public byte getCode() {
		return code;
	}

	public String getType() {
		return type;
	}

	public static CertificateType valueOf(byte code) {
		for (CertificateType t : EnumSet.allOf(CertificateType.class))
			if (code == t.getCode())
				return t;
		return null;
	}

	@Override
	public String toString() {
		return type;
	}
}
