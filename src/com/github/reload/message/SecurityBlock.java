package com.github.reload.message;

import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import com.github.reload.Configuration;
import com.github.reload.message.SecurityBlock.SecurityBlockCodec;
import com.github.reload.net.data.Codec;
import com.github.reload.net.data.ReloadCodec;

/**
 * RELOAD security block
 */
@ReloadCodec(SecurityBlockCodec.class)
public class SecurityBlock {

	private final List<GenericCertificate> certificates;
	private final Signature signature;

	public SecurityBlock(List<GenericCertificate> certificates, Signature signature) {
		super();
		this.certificates = certificates;
		this.signature = signature;
	}

	public static class SecurityBlockCodec extends Codec<SecurityBlock> {

		private static final int CERTS_LENGTH_FIELD = U_INT16;

		private final Codec<GenericCertificate> certCodec;
		private final Codec<Signature> signCodec;

		public SecurityBlockCodec(Configuration conf) {
			super(conf);
			certCodec = getCodec(GenericCertificate.class);
			signCodec = getCodec(Signature.class);
		}

		@Override
		public void encode(SecurityBlock obj, ByteBuf buf, Object... params) throws com.github.reload.net.data.Codec.CodecException {
			Field certsFld = allocateField(buf, CERTS_LENGTH_FIELD);
			for (GenericCertificate c : obj.certificates) {
				certCodec.encode(c, buf);
			}
			certsFld.updateDataLength();

			signCodec.encode(obj.signature, buf);
		}

		@Override
		public SecurityBlock decode(ByteBuf buf, Object... params) throws com.github.reload.net.data.Codec.CodecException {
			List<GenericCertificate> certs = new ArrayList<GenericCertificate>();

			ByteBuf certsBuf = readField(buf, CERTS_LENGTH_FIELD);
			while (certsBuf.readableBytes() > 0) {
				certs.add(certCodec.decode(certsBuf));
			}

			return new SecurityBlock(certs, signCodec.decode(buf));
		}

	}

}
