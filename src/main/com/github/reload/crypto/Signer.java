package com.github.reload.crypto;

import java.security.InvalidKeyException;
import java.security.InvalidParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import com.github.reload.net.encoders.secBlock.HashAlgorithm;
import com.github.reload.net.encoders.secBlock.SignatureAlgorithm;
import com.github.reload.net.encoders.secBlock.SignerIdentity;

public class Signer extends Signature {

	private final Signature signer;
	private final SignerIdentity identity;
	private final HashAlgorithm hashAlg;
	private final SignatureAlgorithm signAlg;

	protected Signer(SignerIdentity identity, HashAlgorithm hashAlg, SignatureAlgorithm signAlg) throws NoSuchAlgorithmException {
		super(hashAlg.toString() + "with" + signAlg.toString());
		this.identity = identity;
		this.hashAlg = hashAlg;
		this.signAlg = signAlg;
		signer = getInstance(hashAlg.toString() + "with" + signAlg.toString());
	}

	@Override
	protected void engineInitSign(PrivateKey privateKey) throws InvalidKeyException {
		if (identity != SignerIdentity.EMPTY_IDENTITY) {
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
		signer.initVerify(publicKey);
	}

	@Override
	protected byte[] engineSign() throws SignatureException {
		return signer.sign();
	}

	@Override
	protected void engineUpdate(byte b) throws SignatureException {
		signer.update(b);
	}

	@Override
	protected void engineUpdate(byte[] b, int off, int len) throws SignatureException {
		signer.update(b, off, len);
	}

	@Override
	protected boolean engineVerify(byte[] sigBytes) throws SignatureException {
		return signer.verify(sigBytes);
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

	/**
	 * @return the identity of the signer
	 */
	public SignerIdentity getIdentity() {
		return identity;
	}

	public SignatureAlgorithm getSignAlgorithm() {
		return signAlg;
	}

	public HashAlgorithm getHashAlgorithm() {
		return hashAlg;
	}

}
