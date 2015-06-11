package com.github.reload.crypto;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.UnpooledByteBufAllocator;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SignatureException;
import com.github.reload.net.encoders.Codec;
import com.github.reload.net.encoders.Codec.CodecException;
import com.github.reload.net.encoders.secBlock.HashAlgorithm;
import com.github.reload.net.encoders.secBlock.Signature;
import com.github.reload.net.encoders.secBlock.SignatureAlgorithm;
import com.github.reload.net.encoders.secBlock.SignerIdentity;

public class Signer {

	private final java.security.Signature signer;
	private final SignerIdentity identity;
	private final HashAlgorithm hashAlg;
	private final SignatureAlgorithm signAlg;

	Signer(SignerIdentity identity, PrivateKey signerKey, HashAlgorithm hashAlg, SignatureAlgorithm signAlg) throws NoSuchAlgorithmException, InvalidKeyException {
		this.identity = identity;
		this.hashAlg = hashAlg;
		this.signAlg = signAlg;
		signer = java.security.Signature.getInstance(hashAlg.toString() + "with" + signAlg.toString());
		if (identity != SignerIdentity.EMPTY_IDENTITY) {
			signer.initSign(signerKey);
		}
	}

	public void update(byte b) throws SignatureException {
		signer.update(b);
	}

	public void update(byte[] b) throws SignatureException {
		signer.update(b);
	}

	public void update(ByteBuf b) throws SignatureException {
		byte[] buf = new byte[b.readableBytes()];
		b.readBytes(buf);
		update(buf);
	}

	public Signature sign() throws SignatureException {
		addSignerIdentity(signer);
		byte[] digest = signer.sign();
		return new Signature(identity, hashAlg, signAlg, digest);
	}

	private void addSignerIdentity(java.security.Signature signer) throws SignatureException {
		ByteBuf b = UnpooledByteBufAllocator.DEFAULT.buffer();
		Codec<SignerIdentity> signIdentityCodec = Codec.getCodec(SignerIdentity.class, null);
		try {
			signIdentityCodec.encode(identity, b);
		} catch (CodecException e) {
			throw new RuntimeException(e);
		}

		signer.update(b.nioBuffer());
		b.release();
	}

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
