package com.github.reload.net.encoders.secBlock;

import io.netty.buffer.ByteBuf;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.List;
import com.github.reload.components.ComponentsContext;
import com.github.reload.net.encoders.Codec;
import com.github.reload.net.encoders.Codec.ReloadCodec;
import com.github.reload.net.encoders.secBlock.SecurityBlock.SecurityBlockCodec;

/**
 * RELOAD security block
 */
@ReloadCodec(SecurityBlockCodec.class)
public class SecurityBlock {

	private final List<GenericCertificate> certificates;
	private final Signature signature;

	public SecurityBlock(List<GenericCertificate> certificates, Signature signature) {
		this.certificates = certificates;
		this.signature = signature;
	}

	public List<? extends Certificate> getCertificates() {
		List<Certificate> out = new ArrayList<Certificate>();
		for (GenericCertificate c : certificates)
			out.add(c.certificate);

		return out;
	}

	public Signature getSignature() {
		return signature;
	}

	static class SecurityBlockCodec extends Codec<SecurityBlock> {

		private static final int CERTS_LENGTH_FIELD = U_INT16;

		private final Codec<GenericCertificate> certCodec;
		private final Codec<Signature> signCodec;

		public SecurityBlockCodec(ComponentsContext ctx) {
			super(ctx);
			certCodec = getCodec(GenericCertificate.class);
			signCodec = getCodec(Signature.class);
		}

		@Override
		public void encode(SecurityBlock obj, ByteBuf buf, Object... params) throws com.github.reload.net.encoders.Codec.CodecException {
			Field certsFld = allocateField(buf, CERTS_LENGTH_FIELD);
			for (GenericCertificate c : obj.certificates) {
				certCodec.encode(c, buf);
			}
			certsFld.updateDataLength();

			signCodec.encode(obj.signature, buf);
		}

		@Override
		public SecurityBlock decode(ByteBuf buf, Object... params) throws com.github.reload.net.encoders.Codec.CodecException {
			List<GenericCertificate> certs = new ArrayList<GenericCertificate>();

			ByteBuf certsBuf = readField(buf, CERTS_LENGTH_FIELD);
			while (certsBuf.readableBytes() > 0) {
				certs.add(certCodec.decode(certsBuf));
			}

			return new SecurityBlock(certs, signCodec.decode(buf));
		}

	}

}
