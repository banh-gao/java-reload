package com.github.reload.crypto;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import com.github.reload.message.NodeID;

/**
 * A wrapper class that provides access to RELOAD values in certificates
 * 
 */
public class ReloadCertificate extends Certificate {

	final Set<NodeID> nodeIds;
	final String username;
	final Certificate certificate;

	public ReloadCertificate(Certificate certificate, String username, Collection<NodeID> nodeIds) {
		super(certificate.getType());
		if (nodeIds == null)
			throw new NullPointerException();
		this.certificate = certificate;
		this.username = username;
		this.nodeIds = new LinkedHashSet<NodeID>(nodeIds);
	}

	/**
	 * @return The RELOAD node-ids of the certificate
	 */
	public Set<NodeID> getNodeIds() {
		return Collections.unmodifiableSet(nodeIds);
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
		return "ReloadCertificate [nodeIds=" + nodeIds + ", username=" + username + ", certType=" + certificate.getType() + "]";
	}

	@Override
	public byte[] getEncoded() throws CertificateEncodingException {
		return certificate.getEncoded();
	}

	@Override
	public PublicKey getPublicKey() {
		return certificate.getPublicKey();
	}

	@Override
	public void verify(PublicKey key) throws CertificateException, NoSuchAlgorithmException, InvalidKeyException, NoSuchProviderException, SignatureException {
		certificate.verify(key);
	}

	@Override
	public void verify(PublicKey key, String sigProvider) throws CertificateException, NoSuchAlgorithmException, InvalidKeyException, NoSuchProviderException, SignatureException {
		certificate.verify(key, sigProvider);
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
