package com.github.reload.message;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import net.sf.jReload.message.DecodingException;
import net.sf.jReload.message.EncUtils;
import net.sf.jReload.message.UnsignedByteBuffer;
import net.sf.jReload.message.UnsignedByteBuffer.Field;

/**
 * Utility class used to encode and decode certificates to RELOAD
 * GenericCertificate specification
 * 
 * @author Daniel Zozin <zdenial@gmx.com>
 * 
 */
public class GenericCertificate {

	private static final int CERT_LENGTH_FIELD = EncUtils.U_INT16;

	private GenericCertificate() {
	}

	/**
	 * Parse a GenericCertificate to a Certificate object
	 * 
	 * @return
	 * @throws CertificateException
	 *             if some error occurs while parsing the certificate
	 */
	public static Certificate parse(UnsignedByteBuffer buf) throws CertificateException {
		CertificateType certType = CertificateType.valueOf(buf.getRaw8());
		if (certType == null)
			throw new CertificateException("Unknown certificate type");

		int length = buf.getLengthValue(CERT_LENGTH_FIELD);
		byte[] enc = new byte[length];
		buf.getRaw(enc);

		InputStream in = new ByteArrayInputStream(enc);
		Certificate cert;

		if (certType == CertificateType.PGP)
			throw new CertificateException("OpenPGP certificate not supported");

		CertificateFactory f = CertificateFactory.getInstance(certType.toString());
		cert = f.generateCertificate(in);

		return cert;
	}

	/**
	 * Write the certificate to the specified buffer in GenericCertificate
	 * format
	 */
	public static void writeGenericTo(Certificate certificate, UnsignedByteBuffer buf) throws CertificateException {
		String type = certificate.getType();
		if (type.equalsIgnoreCase(CertificateType.X509.toString())) {
			buf.putRaw8(CertificateType.X509.getCode());
		} else if (type.equalsIgnoreCase(CertificateType.PGP.toString())) {
			buf.putRaw8(CertificateType.PGP.getCode());
		} else
			throw new DecodingException("Unhandled certificate type");

		byte[] encCert = certificate.getEncoded();

		Field lenFld = buf.allocateLengthField(CERT_LENGTH_FIELD);
		buf.putRaw(encCert);
		lenFld.setEncodedLength(buf.getConsumedFrom(lenFld.getNextPosition()));
	}

	public static int getGenericLength(Certificate certificate) {
		try {
			return EncUtils.U_INT8 + CERT_LENGTH_FIELD + certificate.getEncoded().length;
		} catch (CertificateEncodingException e) {
			throw new RuntimeException(e);
		}
	}
}
