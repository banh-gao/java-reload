package com.github.reload.net.encoders.secBlock;

import io.netty.buffer.ByteBuf;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.EnumSet;
import com.github.reload.Configuration;
import com.github.reload.net.encoders.Codec;
import com.github.reload.net.encoders.Codec.ReloadCodec;
import com.github.reload.net.encoders.secBlock.GenericCertificate.GenericCertificateCodec;

@ReloadCodec(GenericCertificateCodec.class)
public class GenericCertificate {

	public enum CertificateType {
		X509((byte) 0, "X.509"), PGP((byte) 1, "openPGP");

		private final byte code;
		private final String type;

		private CertificateType(byte code, String type) {
			this.code = code;
			this.type = type;
		}

		public byte getCode() {
			return code;
		}

		public String getType() {
			return type;
		}

		public static CertificateType valueOf(byte code) {
			for (CertificateType t : EnumSet.allOf(CertificateType.class))
				if (code == t.getCode())
					return t;
			return null;
		}

		@Override
		public String toString() {
			return type;
		}
	}

	private final CertificateType type;
	private final Certificate certificate;

	public GenericCertificate(CertificateType type, Certificate certificate) {
		this.type = type;
		this.certificate = certificate;
	}

	public static class GenericCertificateCodec extends Codec<GenericCertificate> {

		private static final int CERT_LENGTH_FIELD = U_INT16;

		public GenericCertificateCodec(Configuration conf) {
			super(conf);
		}

		@Override
		public void encode(GenericCertificate obj, ByteBuf buf, Object... params) throws CodecException {
			buf.writeByte(obj.type.getCode());

			byte[] encCert;
			try {
				encCert = obj.certificate.getEncoded();
			} catch (CertificateEncodingException e) {
				throw new CodecException(e);
			}

			Field lenFld = allocateField(buf, CERT_LENGTH_FIELD);
			buf.writeBytes(encCert);
			lenFld.updateDataLength();
		}

		@Override
		public GenericCertificate decode(ByteBuf buf, Object... params) throws CodecException {
			CertificateType certType = CertificateType.valueOf(buf.readByte());

			if (certType == null)
				throw new CodecException("Unknown certificate type");

			CertificateFactory f;
			try {
				f = CertificateFactory.getInstance(certType.toString());
			} catch (CertificateException e) {
				throw new CodecException(e);
			}

			ByteBuf certFld = readField(buf, CERT_LENGTH_FIELD);

			byte[] certData = new byte[certFld.readableBytes()];
			certFld.readBytes(certData);

			try {
				InputStream in = new ByteArrayInputStream(certData);
				return new GenericCertificate(certType, f.generateCertificate(in));
			} catch (CertificateException e) {
				throw new CodecException(e);
			}
		}
	}
}
