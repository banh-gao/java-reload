package com.github.reload.crypto;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.cert.X509Extension;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.security.auth.x500.X500Principal;
import sun.security.x509.GeneralName;
import sun.security.x509.GeneralNames;
import com.github.reload.ReloadOverlay;
import com.github.reload.ReloadUri;
import com.github.reload.message.NodeID;

public class X509CertificateParser implements ReloadCertificateParser {

	private static final Logger logger = Logger.getLogger(ReloadOverlay.class);

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
		Set<NodeID> ids = extractNodeIdFromUris(certificate, overlayName);
		ReloadCertificate reloadCert = new ReloadCertificate(certificate, username, ids);
		if (reloadCert.isSelfSigned())
			checkSelfSigned(reloadCert);

		return reloadCert;
	}

	private static String extractUsernameFromCert(X509Certificate cert) {
		byte[] encGenNames = cert.getExtensionValue(X509Extension.subjectAlternativeName.getId());

		if (encGenNames == null)
			return "";

		ASN1Primitive derObject = toDERObject(encGenNames);

		if (derObject instanceof DEROctetString) {
			DEROctetString derOctetString = (DEROctetString) derObject;

			derObject = toDERObject(derOctetString.getOctets());
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
				logger.log(Priority.WARN, e);
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
		int i = 1;
		for (NodeID certId : reloadCert.getNodeIds()) {
			NodeID computedId = X509Utils.getKeyBasedNodeId(i, reloadCert.getPublicKey().getEncoded(), certId.getData().length);
			if (!computedId.equals(certId))
				throw new CertificateException("Illegal node-id at index " + i + " for self-signed certificate");
			i++;
		}
	}

	private static Set<NodeID> extractNodeIdFromUris(X509Certificate cert, String overlayName) throws CertificateException {
		Set<NodeID> ids = new LinkedHashSet<NodeID>();
		for (ReloadUri uri : extractUris(cert)) {
			if (overlayName == null || uri.getOverlayName().equalsIgnoreCase(overlayName)) {
				ids.add(uri.getDestinationList().getNodeDestination());
			}
		}

		if (ids.size() == 0)
			throw new CertificateException("No nodeid found");

		return ids;
	}

	private static Set<ReloadUri> extractUris(X509Certificate cert) throws CertificateException {
		byte[] encGenNames = cert.getExtensionValue(X509Extension.subjectAlternativeName.getId());
		if (encGenNames == null)
			throw new CertificateException("Invalid reload certificate: no node-id found");

		ASN1Primitive derObject = toDERObject(encGenNames);

		if (derObject instanceof DEROctetString) {
			DEROctetString derOctetString = (DEROctetString) derObject;

			derObject = toDERObject(derOctetString.getOctets());
		}

		Set<ReloadUri> out = new LinkedHashSet<ReloadUri>();

		for (GeneralName name : GeneralNames.getInstance(derObject).getNames()) {
			if (name.getTagNo() == GeneralName.uniformResourceIdentifier) {
				try {
					out.add(ReloadUri.create(name.getName().toString()));
				} catch (URISyntaxException e) {
					logger.log(Priority.DEBUG, "Invalid RELOAD URI: " + name.getName().toString());
				}
			}
		}

		if (out.isEmpty())
			throw new CertificateException("No valid RELOAD URI found");

		return out;
	}

	private static ASN1Primitive toDERObject(byte[] data) {
		ByteArrayInputStream inStream = new ByteArrayInputStream(data);
		ASN1InputStream asnInputStream = new ASN1InputStream(inStream);
		ASN1Primitive p;
		try {
			p = asnInputStream.readObject();
		} catch (IOException e) {
			try {
				asnInputStream.close();
				inStream.close();
			} catch (IOException _) {
				// Ignored
			}
			throw new RuntimeException(e);
		}

		try {
			asnInputStream.close();
			inStream.close();
		} catch (IOException _) {
			// Ignored
		}

		return p;
	}
}
