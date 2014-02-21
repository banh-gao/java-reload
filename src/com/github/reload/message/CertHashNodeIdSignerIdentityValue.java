package com.github.reload.message;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import net.sf.jReload.message.EncUtils;
import net.sf.jReload.message.NodeID;
import net.sf.jReload.message.UnsignedByteBuffer;
import net.sf.jReload.message.UnsignedByteBuffer.Field;

/**
 * Indentity value that contains the hashed signer certificate and the nodeid
 * 
 * @author Daniel Zozin <zdenial@gmx.com>
 * 
 */
public class CertHashNodeIdSignerIdentityValue extends SignerIdentityValue {

	private final int CERT_HASH_NODEID_LENGTH_FIELD = EncUtils.U_INT8;

	private final byte[] certHash;

	public CertHashNodeIdSignerIdentityValue(UnsignedByteBuffer buf, HashAlgorithm certHashAlg) {
		super(certHashAlg);
		int len = buf.getLengthValue(CERT_HASH_NODEID_LENGTH_FIELD);

		certHash = new byte[len];

		buf.getRaw(certHash);
	}

	public CertHashNodeIdSignerIdentityValue(HashAlgorithm certHashAlg, Certificate identityCertificate, NodeID signerNodeId) {
		super(certHashAlg);
		certHash = computeHash(certHashAlg, identityCertificate, signerNodeId);
	}

	public static byte[] computeHash(HashAlgorithm certHashAlg, Certificate identityCertificate, NodeID signerNodeId) {
		try {
			MessageDigest md = MessageDigest.getInstance(certHashAlg.toString());
			md.update(signerNodeId.getData());
			return md.digest(identityCertificate.getEncoded());
		} catch (CertificateEncodingException e) {
			throw new RuntimeException(e);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	protected void implWriteTo(UnsignedByteBuffer buf) {
		Field lenFld = buf.allocateLengthField(CERT_HASH_NODEID_LENGTH_FIELD);
		buf.putRaw(certHash);

		lenFld.setEncodedLength(buf.getConsumedFrom(lenFld.getNextPosition()));
	}

	@Override
	public byte[] getHashValue() {
		return certHash;
	}
}