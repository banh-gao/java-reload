package com.github.reload.message;

import java.security.InvalidKeyException;
import java.security.InvalidParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.util.Arrays;
import com.github.reload.net.data.Codec;

public class GenericSignature extends Signature implements Cloneable {

	public static GenericSignature EMPTY_SIGNATURE;

	static {
		try {
			EMPTY_SIGNATURE = new GenericSignature(SignerIdentity.EMPTY_IDENTITY, HashAlgorithm.NONE, SignatureAlgorithm.ANONYMOUS);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}

	private Signature signer;

	private final SignerIdentity signerIdentity;

	private final HashAlgorithm hashAlg;
	private final SignatureAlgorithm signAlg;

	private byte[] digest;

	private GenericSignature(byte[] digest, SignerIdentity identity, HashAlgorithm hashAlg, SignatureAlgorithm signAlg) throws NoSuchAlgorithmException {
		super(hashAlg.toString() + "with" + signAlg.toString());
		this.hashAlg = hashAlg;
		this.signAlg = signAlg;
		this.signerIdentity = identity;
		this.digest = digest;

		if (getAlgorithm() != null) {
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
		this.hashAlg = signHashAlg;
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
		return hashAlg;
	}

	public byte[] getDigest() {
		return digest;
	}

	@Override
	public String toString() {
		StringBuilder out = new StringBuilder();
		out.append("GenericSignature[hashAlg=" + hashAlg + ',');
		out.append("signAlg=" + signAlg + ',');
		out.append("signerID=" + signerIdentity + ',');
		out.append("digest=" + Codec.hexDump(digest) + ']');
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
			s = new GenericSignature(signerIdentity, hashAlg, signAlg);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
		s.digest = Arrays.copyOf(digest, digest.length);
		return s;
	}
}
