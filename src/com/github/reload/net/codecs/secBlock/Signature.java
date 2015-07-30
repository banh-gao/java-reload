package com.github.reload.net.codecs.secBlock;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.UnpooledByteBufAllocator;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SignatureException;
import java.util.Arrays;
import java.util.Objects;
import com.github.reload.components.ComponentsContext;
import com.github.reload.net.codecs.Codec;
import com.github.reload.net.codecs.Codec.CodecException;
import com.github.reload.net.codecs.Codec.ReloadCodec;
import com.github.reload.net.codecs.secBlock.Signature.SignatureCodec;

@ReloadCodec(SignatureCodec.class)
public class Signature {

	public static Signature EMPTY_SIGNATURE = new Signature(SignerIdentity.EMPTY_IDENTITY, HashAlgorithm.NONE, SignatureAlgorithm.ANONYMOUS, new byte[0]);

	private final SignerIdentity signerIdentity;

	private final HashAlgorithm hashAlg;
	private final SignatureAlgorithm signAlg;

	private final byte[] digest;

	private Signature(byte[] digest, SignerIdentity identity, HashAlgorithm hashAlg, SignatureAlgorithm signAlg) {
		this.hashAlg = hashAlg;
		this.signAlg = signAlg;
		signerIdentity = identity;
		this.digest = digest;
	}

	/**
	 * Create a signature object for the purpose of signature generation
	 * 
	 * @param signerIdentity
	 * @param signHashAlg
	 * @param signAlg
	 * @throws NoSuchAlgorithmException
	 */
	public Signature(SignerIdentity signerIdentity, HashAlgorithm signHashAlg, SignatureAlgorithm signAlg, byte[] digest) {
		this.signAlg = signAlg;
		hashAlg = signHashAlg;
		this.signerIdentity = signerIdentity;
		this.digest = digest;
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

	/**
	 * Verify this signature over the passed data. Note that the signer identity
	 * will be automatically added to the given data before signature
	 * verification
	 * 
	 * @param data
	 * @param publicKey
	 * @return
	 * @throws GeneralSecurityException
	 */
	public boolean verify(ByteBuf data, PublicKey publicKey) throws GeneralSecurityException {
		java.security.Signature s = java.security.Signature.getInstance(hashAlg.toString() + "with" + signAlg.toString());
		s.initVerify(publicKey);
		s.update(data.nioBuffer());

		addSignerIdentity(s);

		return s.verify(digest);
	}

	private void addSignerIdentity(java.security.Signature signer) throws SignatureException {
		ByteBuf b = UnpooledByteBufAllocator.DEFAULT.buffer();
		Codec<SignerIdentity> signIdentityCodec = Codec.getCodec(SignerIdentity.class, null);
		try {
			signIdentityCodec.encode(signerIdentity, b);
		} catch (CodecException e) {
			throw new RuntimeException(e);
		}

		signer.update(b.nioBuffer());
		b.release();
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), signerIdentity, hashAlg, signAlg, digest);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Signature other = (Signature) obj;
		if (!Arrays.equals(digest, other.digest))
			return false;
		if (hashAlg != other.hashAlg)
			return false;
		if (signAlg != other.signAlg)
			return false;
		if (signerIdentity == null) {
			if (other.signerIdentity != null)
				return false;
		} else if (!signerIdentity.equals(other.signerIdentity))
			return false;
		return true;
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

	static class SignatureCodec extends Codec<Signature> {

		private static final int DIGEST_LENGTH_FIELD = U_INT16;

		private final Codec<SignatureAlgorithm> signAlgCodec;
		private final Codec<HashAlgorithm> hashAlgCodec;
		private final Codec<SignerIdentity> signIdentityCodec;

		public SignatureCodec(ComponentsContext ctx) {
			super(ctx);
			signAlgCodec = getCodec(SignatureAlgorithm.class);
			hashAlgCodec = getCodec(HashAlgorithm.class);
			signIdentityCodec = getCodec(SignerIdentity.class);
		}

		@Override
		public void encode(Signature obj, ByteBuf buf, Object... params) throws CodecException {
			hashAlgCodec.encode(obj.hashAlg, buf);
			signAlgCodec.encode(obj.signAlg, buf);
			signIdentityCodec.encode(obj.signerIdentity, buf);

			Field lenFld = allocateField(buf, DIGEST_LENGTH_FIELD);
			buf.writeBytes(obj.digest);
			lenFld.updateDataLength();
		}

		@Override
		public Signature decode(ByteBuf buf, Object... params) throws CodecException {
			HashAlgorithm hashAlg = hashAlgCodec.decode(buf);
			SignatureAlgorithm signAlg = signAlgCodec.decode(buf);
			SignerIdentity identity = signIdentityCodec.decode(buf);

			ByteBuf digestData = readField(buf, DIGEST_LENGTH_FIELD);

			byte[] digest = new byte[digestData.readableBytes()];
			digestData.readBytes(digest);
			digestData.release();

			return new Signature(digest, identity, hashAlg, signAlg);
		}
	}
}
