package com.github.reload.crypto;

import java.security.cert.Certificate;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import com.github.reload.net.codecs.header.NodeID;

/**
 * A wrapper class that provides access to RELOAD values in certificates
 * 
 */
public class ReloadCertificate {

	final NodeID nodeId;
	final String username;
	final Certificate certificate;

	public ReloadCertificate(Certificate certificate, String username, NodeID nodeId) {
		if (nodeId == null)
			throw new NullPointerException();
		this.certificate = certificate;
		this.username = username;
		this.nodeId = nodeId;
	}

	/**
	 * @return The RELOAD node-id of the certificate
	 */
	public NodeID getNodeId() {
		return nodeId;
	}

	/**
	 * @return The username contained in the certificate or empty string if not
	 *         found
	 */
	public String getUsername() {
		return username;
	}

	/**
	 * @return The unparsed original certificate
	 */
	public Certificate getOriginalCertificate() {
		return certificate;
	}

	public boolean isSelfSigned() {
		if (certificate instanceof X509Certificate) {
			X509Certificate cert = (X509Certificate) certificate;
			return cert.getSubjectX500Principal().equals(cert.getIssuerX500Principal());
		}
		throw new IllegalStateException("Unknow certificate type");
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((certificate == null) ? 0 : certificate.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ReloadCertificate other = (ReloadCertificate) obj;
		if (certificate == null) {
			if (other.certificate != null)
				return false;
		} else if (!certificate.equals(other.certificate))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "ReloadCertificate [nodeId=" + nodeId + ", username=" + username + ", certType=" + certificate.getType() + "]";
	}

	/**
	 * Checks that the certificate is currently valid. It is if the current date
	 * and time are within the validity period given in the certificate.
	 * 
	 * @throws CertificateExpiredException
	 *             if the certificate has expired.
	 * @throws CertificateNotYetValidException
	 *             if the certificate is not yet valid.
	 */
	public void checkValidity() throws CertificateExpiredException, CertificateNotYetValidException {
		if (certificate instanceof X509Certificate) {
			((X509Certificate) certificate).checkValidity();
			return;
		}
		throw new RuntimeException("Unsupported certificate type");
	}
}
