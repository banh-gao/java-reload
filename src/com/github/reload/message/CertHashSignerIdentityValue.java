package com.github.reload.message;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import net.sf.jReload.message.EncUtils;
import net.sf.jReload.message.UnsignedByteBuffer;
import net.sf.jReload.message.UnsignedByteBuffer.Field;

/**
 * Hash value that contains the signer certificate
 * 
 * @author Daniel Zozin <zdenial@gmx.com>
 * 
 */
public class CertHashSignerIdentityValue extends SignerIdentityValue {

	private final int CERT_HASH_LENGTH_FIELD = EncUtils.U_INT8;

	private final byte[] certHash;

	public CertHashSignerIdentityValue(UnsignedByteBuffer buf, HashAlgorithm certHashAlg) {
		super(certHashAlg);
		int len = buf.getLengthValue(CERT_HASH_LENGTH_FIELD);
		certHash = new byte[len];
		buf.getRaw(certHash);
	}

	public CertHashSignerIdentityValue(HashAlgorithm certHashAlg, Certificate identityCertificate) {
		super(certHashAlg);
		certHash = computeHash(certHashAlg, identityCertificate);

	}

	public static byte[] computeHash(HashAlgorithm certHashAlg, Certificate identityCertificate) {
		try {
			MessageDigest md = MessageDigest.getInstance(certHashAlg.toString());
			return md.digest(identityCertificate.getEncoded());
		} catch (CertificateEncodingException e) {
			throw new RuntimeException(e);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	protected void implWriteTo(UnsignedByteBuffer buf) {
		Field lenFld = buf.allocateLengthField(CERT_HASH_LENGTH_FIELD);
		buf.putRaw(certHash);
		lenFld.setEncodedLength(buf.getConsumedFrom(lenFld.getNextPosition()));
	}

	@Override
	public byte[] getHashValue() {
		return certHash;
	}
}