package com.github.reload.message;

import io.netty.buffer.ByteBuf;
import java.security.NoSuchAlgorithmException;
import com.github.reload.Context;
import com.github.reload.message.Signature.SignatureCodec;
import com.github.reload.net.data.Codec;
import com.github.reload.net.data.ReloadCodec;

@ReloadCodec(SignatureCodec.class)
public class Signature {

	public static Signature EMPTY_SIGNATURE;

	static {
		try {
			EMPTY_SIGNATURE = new Signature(SignerIdentity.EMPTY_IDENTITY, HashAlgorithm.NONE, SignatureAlgorithm.ANONYMOUS, new byte[0]);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}

	private final SignerIdentity signerIdentity;

	private final HashAlgorithm hashAlg;
	private final SignatureAlgorithm signAlg;

	private byte[] digest;

	private Signature(byte[] digest, SignerIdentity identity, HashAlgorithm hashAlg, SignatureAlgorithm signAlg) throws NoSuchAlgorithmException {
		this.hashAlg = hashAlg;
		this.signAlg = signAlg;
		this.signerIdentity = identity;
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
	public Signature(SignerIdentity signerIdentity, HashAlgorithm signHashAlg, SignatureAlgorithm signAlg, byte[] digest) throws NoSuchAlgorithmException {
		this.signAlg = signAlg;
		this.hashAlg = signHashAlg;
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

	@Override
	public String toString() {
		StringBuilder out = new StringBuilder();
		out.append("GenericSignature[hashAlg=" + hashAlg + ',');
		out.append("signAlg=" + signAlg + ',');
		out.append("signerID=" + signerIdentity + ',');
		out.append("digest=" + Codec.hexDump(digest) + ']');
		return out.toString();
	}

	public static class SignatureCodec extends Codec<Signature> {

		private static final int DIGEST_LENGTH_FIELD = U_INT16;

		private Codec<SignatureAlgorithm> signAlgCodec;
		private Codec<HashAlgorithm> hashAlgCodec;
		private Codec<SignerIdentity> signIdentityCodec;

		public SignatureCodec(Context context) {
			super(context);
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

			try {
				return new Signature(digest, identity, hashAlg, signAlg);
			} catch (NoSuchAlgorithmException e) {
				throw new CodecException(e);
			}
		}
	}
}
