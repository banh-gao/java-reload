package com.github.reload.net.encoders.secBlock;

import io.netty.buffer.ByteBuf;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import com.github.reload.components.ComponentsContext;
import com.github.reload.net.encoders.Codec;
import com.github.reload.net.encoders.Codec.ReloadCodec;
import com.github.reload.net.encoders.header.NodeID;
import com.github.reload.net.encoders.secBlock.CertHashNodeIdSignerIdentityValue.CertHashNodeIdSignerIdentityValueCodec;

@ReloadCodec(CertHashNodeIdSignerIdentityValueCodec.class)
public class CertHashNodeIdSignerIdentityValue extends SignerIdentityValue {

	private final HashAlgorithm certHashAlg;
	private final byte[] certHash;

	public CertHashNodeIdSignerIdentityValue(HashAlgorithm certHashAlg, byte[] certHash) {
		super();
		this.certHashAlg = certHashAlg;
		this.certHash = certHash;
	}

	public CertHashNodeIdSignerIdentityValue(HashAlgorithm certHashAlg, Certificate identityCertificate, NodeID signerNodeId) {
		this.certHashAlg = certHashAlg;
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
	public HashAlgorithm getHashAlgorithm() {
		return certHashAlg;
	}

	@Override
	public byte[] getHashValue() {
		return certHash;
	}

	static class CertHashNodeIdSignerIdentityValueCodec extends Codec<CertHashNodeIdSignerIdentityValue> {

		private final int CERT_HASH_NODEID_LENGTH_FIELD = U_INT8;

		public CertHashNodeIdSignerIdentityValueCodec(ComponentsContext ctx) {
			super(ctx);
		}

		@Override
		public void encode(CertHashNodeIdSignerIdentityValue obj, ByteBuf buf, Object... params) throws CodecException {
			buf.writeByte(obj.certHashAlg.getCode());

			Field lenFld = allocateField(buf, CERT_HASH_NODEID_LENGTH_FIELD);
			buf.writeBytes(obj.certHash);

			lenFld.updateDataLength();
		}

		@Override
		public CertHashNodeIdSignerIdentityValue decode(ByteBuf buf, Object... params) throws CodecException {
			HashAlgorithm certHashAlg = HashAlgorithm.valueOf(buf.readByte());

			if (certHashAlg == null)
				throw new CodecException("Unsupported hash algorithm");

			ByteBuf dataFld = readField(buf, CERT_HASH_NODEID_LENGTH_FIELD);

			byte[] certHash = new byte[dataFld.readableBytes()];

			dataFld.readBytes(certHash);

			return new CertHashNodeIdSignerIdentityValue(certHashAlg, certHash);
		}
	}
}