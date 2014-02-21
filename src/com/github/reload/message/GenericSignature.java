package com.github.reload.message;

import java.security.InvalidKeyException;
import java.security.InvalidParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.util.Arrays;
import net.sf.jReload.ReloadOverlay;
import net.sf.jReload.message.DecodingException;
import net.sf.jReload.message.EncUtils;
import net.sf.jReload.message.UnsignedByteBuffer;
import net.sf.jReload.message.UnsignedByteBuffer.Field;
import org.apache.log4j.Logger;
import org.apache.log4j.Priority;

/**
 * A RELOAD generic signature structure
 * 
 * @author Daniel Zozin <zdenial@gmx.com>
 * 
 */
public class GenericSignature extends Signature implements Cloneable {

	private static final Logger logger = Logger.getLogger(ReloadOverlay.class);

	public static GenericSignature EMPTY_SIGNATURE;

	static {
		try {
			EMPTY_SIGNATURE = new GenericSignature(SignerIdentity.EMPTY_IDENTITY, HashAlgorithm.NONE, SignatureAlgorithm.ANONYMOUS);
		} catch (NoSuchAlgorithmException e) {
			logger.log(Priority.FATAL, e);
		}
	}

	private static final int DIGEST_LENGTH_FIELD = EncUtils.U_INT16;

	private Signature signer;

	private final SignerIdentity signerIdentity;

	private final HashAlgorithm signHashAlg;
	private final SignatureAlgorithm signAlg;

	private byte[] digest;

	/**
	 * Parse and initialize a generic signature for the purpose of signature
	 * verifing
	 * 
	 * @param buf
	 * @return
	 * @throws NoSuchAlgorithmException
	 */
	public static GenericSignature parse(UnsignedByteBuffer buf) throws NoSuchAlgorithmException {
		HashAlgorithm hashAlg = readHashAlg(buf);
		SignatureAlgorithm signAlg = readSignAlg(buf);
		return new GenericSignature(buf, hashAlg, signAlg);
	}

	private GenericSignature(UnsignedByteBuffer buf, HashAlgorithm hashAlg, SignatureAlgorithm signAlg) throws NoSuchAlgorithmException {
		super(hashAlg.toString() + "with" + signAlg.toString());
		signHashAlg = hashAlg;
		this.signAlg = signAlg;

		signerIdentity = SignerIdentity.parse(buf);

		int len = buf.getLengthValue(DIGEST_LENGTH_FIELD);

		digest = new byte[len];
		buf.getRaw(digest);

		if (signerIdentity != SignerIdentity.EMPTY_IDENTITY) {
			signer = Signature.getInstance(getAlgorithm());
		}
	}

	/**
	 * Create a signature object for the purpose of signature generation
	 * 
	 * @param signerIdentity
	 * @param signHashAlg
	 * @param signAlg
	 * @throws NoSuchAlgorithmException
	 */
	public GenericSignature(SignerIdentity signerIdentity, HashAlgorithm signHashAlg, SignatureAlgorithm signAlg) throws NoSuchAlgorithmException {
		super(signHashAlg.toString() + "with" + signAlg.toString());

		this.signAlg = signAlg;
		this.signHashAlg = signHashAlg;
		this.signerIdentity = signerIdentity;
		digest = new byte[0];

		if (signerIdentity != SignerIdentity.EMPTY_IDENTITY) {
			signer = Signature.getInstance(signHashAlg.toString() + "with" + signAlg.toString());
		}
	}

	/**
	 * @return the identity of the signer
	 */
	public SignerIdentity getIdentity() {
		return signerIdentity;
	}

	public SignatureAlgorithm getSignAlg() {
		return signAlg;
	}

	public HashAlgorithm getSignHashAlg() {
		return signHashAlg;
	}

	public byte[] getDigest() {
		return digest;
	}

	/**
	 * Format defined in SignAndHashAlgorithm in RFC 5246
	 */
	private static HashAlgorithm readHashAlg(UnsignedByteBuffer buf) {
		HashAlgorithm hashAlg = HashAlgorithm.valueOf(buf.getRaw8());
		if (hashAlg == null)
			throw new DecodingException("Unsupported hash algorithm");

		return hashAlg;
	}

	/**
	 * Format defined in SignAndHashAlgorithm in RFC 5246
	 */
	private static SignatureAlgorithm readSignAlg(UnsignedByteBuffer buf) {
		SignatureAlgorithm signAlg = SignatureAlgorithm.valueOf(buf.getRaw8());
		if (signAlg == null)
			throw new DecodingException("Unsupported signature algorithm");
		return signAlg;
	}

	public void writeTo(UnsignedByteBuffer buf) {
		buf.putRaw8(signHashAlg.getCode());
		buf.putRaw8(signAlg.getCode());
		signerIdentity.writeTo(buf);
		Field lenFld = buf.allocateLengthField(DIGEST_LENGTH_FIELD);
		buf.putRaw(digest);
		lenFld.setEncodedLength(buf.getConsumedFrom(lenFld.getNextPosition()));
	}

	@Override
	public String toString() {
		StringBuilder out = new StringBuilder();
		out.append("GenericSignature[hashAlg=" + signHashAlg + ',');
		out.append("signAlg=" + signAlg + ',');
		out.append("signerID=" + signerIdentity + ',');
		out.append("digest=" + EncUtils.toHexString(digest) + ']');
		return out.toString();
	}

	@Override
	protected void engineInitSign(PrivateKey privateKey) throws InvalidKeyException {
		if (signerIdentity != SignerIdentity.EMPTY_IDENTITY) {
			signer.initSign(privateKey);
		}
	}

	/**
	 * Create a signature validator that can be used to validate the data signed
	 * with this signature
	 * 
	 * @param signerCertificate
	 *            the certificate used to initialize the validator
	 * @return The validator for this signature
	 * @throws SignatureException
	 */
	@Override
	protected void engineInitVerify(PublicKey publicKey) throws InvalidKeyException {
		if (signerIdentity != SignerIdentity.EMPTY_IDENTITY) {
			signer.initVerify(publicKey);
		}
	}

	@Override
	protected byte[] engineSign() throws SignatureException {
		if (signerIdentity != SignerIdentity.EMPTY_IDENTITY) {
			digest = signer.sign();
		}
		return digest;
	}

	@Override
	protected void engineUpdate(byte b) throws SignatureException {
		if (signerIdentity != SignerIdentity.EMPTY_IDENTITY) {
			signer.update(b);
		}
	}

	@Override
	protected void engineUpdate(byte[] b, int off, int len) throws SignatureException {
		if (signerIdentity != SignerIdentity.EMPTY_IDENTITY) {
			signer.update(b, off, len);
		}
	}

	@Override
	protected boolean engineVerify(byte[] sigBytes) throws SignatureException {
		if (sigBytes != null)
			throw new IllegalArgumentException("Must be null");

		if (signerIdentity != SignerIdentity.EMPTY_IDENTITY)
			return signer.verify(digest);

		return true;
	}

	@Override
	@Deprecated
	protected void engineSetParameter(String param, Object value) throws InvalidParameterException {
		// Ignored
	}

	@Override
	@Deprecated
	protected Object engineGetParameter(String arg0) throws InvalidParameterException {
		// Ignored
		return null;
	}

	@Override
	public GenericSignature clone() {
		GenericSignature s;
		try {
			s = new GenericSignature(signerIdentity, signHashAlg, signAlg);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
		s.digest = Arrays.copyOf(digest, digest.length);
		return s;
	}
}
