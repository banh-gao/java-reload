package com.github.reload.crypto;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;
import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.security.auth.x500.X500Principal;
import org.apache.log4j.Logger;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import com.github.reload.ReloadOverlay;
import com.github.reload.ReloadUri;
import com.github.reload.net.encoders.header.NodeID;

public class X509CertificateParser implements ReloadCertificateParser {

	private static final Logger logger = Logger.getLogger(ReloadOverlay.class);

	private static final String SDN_OID = "2.5.29.17";

	protected X509CertificateParser() {
	}

	@Override
	public ReloadCertificate parse(Certificate certificate) throws CertificateException {
		if (!(certificate instanceof X509Certificate))
			throw new IllegalArgumentException("Invalid X.509 certificate");
		return parse((X509Certificate) certificate);
	}

	/**
	 * Parse the given certificate as a Reload certificate
	 * 
	 * @param certificate
	 *            the certificate to be parsed
	 * @return The parsed certificate
	 * @throws CertificateException
	 *             If the certificate parsing fails
	 */
	public static ReloadCertificate parse(X509Certificate certificate) throws CertificateException {
		return parse(certificate, null);
	}

	/**
	 * Parse the given certificate as a Reload certificate
	 * 
	 * @param certificate
	 *            the certificate to be parsed
	 * @param overlayName
	 *            the name of the overlay to be used to filter only the matching
	 *            node-ids, if null all the node-ids will be parsed
	 * @return The parsed certificate
	 * @throws CertificateException
	 *             If the certificate parsing fails
	 */
	public static ReloadCertificate parse(X509Certificate certificate, String overlayName) throws CertificateException {
		String username = extractUsernameFromCert(certificate);
		NodeID id = extractNodeIdFromUri(certificate, overlayName);
		ReloadCertificate reloadCert = new ReloadCertificate(certificate, username, id);
		if (reloadCert.isSelfSigned()) {
			checkSelfSigned(reloadCert);
		}

		return reloadCert;
	}

	private static String extractUsernameFromCert(X509Certificate cert) throws CertificateException {
		byte[] encGenNames = cert.getExtensionValue(SDN_OID);

		if (encGenNames == null)
			return "";

		DERObject derObject;
		try {
			derObject = toDERObject(encGenNames);
		} catch (IOException e) {
			throw new CertificateException(e);
		}

		for (GeneralName name : GeneralNames.getInstance(derObject).getNames()) {
			if (name.getTagNo() == GeneralName.rfc822Name)
				return name.getName().toString();
		}

		return "";
	}

	static X509Certificate getMatchingCertificate(X500Principal principal, List<? extends Certificate> availableCerts) {
		for (Certificate c : availableCerts) {
			if (!(c instanceof X509Certificate))
				throw new IllegalArgumentException("Invalid X509 certificate");
			X509Certificate cert = (X509Certificate) c;
			try {
				if (isDNmatching(cert.getSubjectX500Principal(), principal))
					return cert;
			} catch (InvalidNameException e) {
				logger.warn(e.getMessage());
			}
		}
		return null;
	}

	/**
	 * @return true if the distinguished names of the two principal matches
	 * @throws InvalidNameException
	 */
	private static boolean isDNmatching(X500Principal p1, X500Principal p2) throws InvalidNameException {
		List<Rdn> rdn1 = new LdapName(p1.getName()).getRdns();
		List<Rdn> rdn2 = new LdapName(p2.getName()).getRdns();

		if (rdn1.size() != rdn2.size())
			return false;

		return rdn1.containsAll(rdn2);
	}

	/**
	 * Check whether the node-ids in the certificate are derived from the
	 * certificate public key, if some id doesn't match, a CertificateException
	 * will be thrown
	 * 
	 * @param reloadCert
	 * @throws CertificateException
	 */
	private static void checkSelfSigned(ReloadCertificate reloadCert) throws CertificateException {
		NodeID certId = reloadCert.getNodeId();
		NodeID computedId = X509Utils.getKeyBasedNodeId(1, reloadCert.getPublicKey().getEncoded(), certId.getData().length);

		if (!computedId.equals(certId))
			throw new CertificateException("Illegal node-id at index " + 1 + " for self-signed certificate");
	}

	private static NodeID extractNodeIdFromUri(X509Certificate cert, String overlayName) throws CertificateException {
		ReloadUri uri = extractUri(cert);
		if (overlayName == null || uri.getOverlayName().equalsIgnoreCase(overlayName)) {
			return uri.getDestinationList().getNodeDestination();
		}

		throw new CertificateException("No nodeid found");
	}

	private static ReloadUri extractUri(X509Certificate cert) throws CertificateException {
		byte[] encGenNames = cert.getExtensionValue(SDN_OID);
		if (encGenNames == null)
			throw new CertificateException("Invalid reload certificate: no node-id found");

		DERObject derObject;
		try {
			derObject = toDERObject(encGenNames);
		} catch (IOException e) {
			throw new CertificateException(e);
		}

		for (GeneralName name : GeneralNames.getInstance(derObject).getNames()) {
			if (name.getTagNo() == GeneralName.uniformResourceIdentifier) {
				try {
					return ReloadUri.create(name.getName().toString());
				} catch (URISyntaxException e) {
					logger.warn("Invalid RELOAD URI: " + name.getName().toString(), e);
				}
			}
		}

		throw new CertificateException("No valid RELOAD URI found");
	}

	private static DERObject toDERObject(byte[] data) throws IOException {
		ByteArrayInputStream inStream = new ByteArrayInputStream(data);
		ASN1InputStream DIS = new ASN1InputStream(inStream);
		try {
			DERObject out = DIS.readObject();

			if (out instanceof DEROctetString) {
				DEROctetString derOctetString = (DEROctetString) out;
				out = toDERObject(derOctetString.getOctets());
			}
			return out;
		} finally {
			DIS.close();
			inStream.close();
		}
	}
}
