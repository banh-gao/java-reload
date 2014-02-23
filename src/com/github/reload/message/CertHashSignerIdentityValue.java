package com.github.reload.message;

import io.netty.buffer.ByteBuf;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import com.github.reload.Context;
import com.github.reload.message.CertHashSignerIdentityValue.CertHashSignerIdentityValueCodec;
import com.github.reload.net.data.Codec;
import com.github.reload.net.data.ReloadCodec;

@ReloadCodec(CertHashSignerIdentityValueCodec.class)
public class CertHashSignerIdentityValue extends SignerIdentityValue {

	private final HashAlgorithm certHashAlg;
	private final byte[] certHash;

	public CertHashSignerIdentityValue(HashAlgorithm certHashAlg, byte[] certHash) {
		this.certHashAlg = certHashAlg;
		this.certHash = certHash;
	}

	public CertHashSignerIdentityValue(HashAlgorithm certHashAlg, Certificate identityCertificate) {
		this.certHashAlg = certHashAlg;
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
	public HashAlgorithm getHashAlgorithm() {
		return certHashAlg;
	}

	@Override
	public byte[] getHashValue() {
		return certHash;
	}

	public static class CertHashSignerIdentityValueCodec extends Codec<CertHashSignerIdentityValue> {

		private final int CERT_HASH_LENGTH_FIELD = U_INT8;

		public CertHashSignerIdentityValueCodec(Context context) {
			super(context);
			// TODO Auto-generated constructor stub
		}

		@Override
		public void encode(CertHashSignerIdentityValue obj, ByteBuf buf, Object... params) throws CodecException {
			buf.writeByte(obj.certHashAlg.getCode());

			Field lenFld = allocateField(buf, CERT_HASH_LENGTH_FIELD);
			buf.writeBytes(obj.certHash);
			lenFld.updateDataLength();
		}

		@Override
		public CertHashSignerIdentityValue decode(ByteBuf buf, Object... params) throws CodecException {
			HashAlgorithm certHashAlg = HashAlgorithm.valueOf(buf.readByte());

			if (certHashAlg == null)
				throw new CodecException("Unsupported hash algorithm");

			ByteBuf hashFld = readField(buf, CERT_HASH_LENGTH_FIELD);

			byte[] certHash = new byte[hashFld.readableBytes()];

			hashFld.readBytes(certHash);

			return new CertHashSignerIdentityValue(certHashAlg, certHash);
		}

	}

}