package com.github.reload.crypto;

import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.net.ssl.SSLEngine;
import com.github.reload.Bootstrap;
import com.github.reload.components.ComponentsContext.CompStart;
import com.github.reload.components.ComponentsRepository.Component;
import com.github.reload.conf.Configuration;
import com.github.reload.net.encoders.header.NodeID;
import com.github.reload.net.encoders.secBlock.HashAlgorithm;
import com.github.reload.net.encoders.secBlock.SignatureAlgorithm;
import com.github.reload.net.encoders.secBlock.SignerIdentity;
import com.github.reload.net.ice.HostCandidate.OverlayLinkType;

/**
 * Helper class that provides the cryptographic functionalities for the local
 * node
 * 
 */
@Component(CryptoHelper.class)
public abstract class CryptoHelper<T extends Certificate> {

	/**
	 * The hashing algorithm of the overlay
	 */
	public static final HashAlgorithm OVERLAY_HASHALG = HashAlgorithm.SHA1;

	@Component
	private Bootstrap boot;

	@Component
	private Keystore keystore;

	private HashAlgorithm signHashAlg;
	private SignatureAlgorithm signAlg;
	private HashAlgorithm certHashAlg;

	@CompStart
	public void init() {
		signHashAlg = boot.getSignHashAlg();
		signAlg = boot.getSignAlg();
		certHashAlg = boot.getHashAlg();
	}

	public HashAlgorithm getSignHashAlg() {
		return signHashAlg;
	}

	public SignatureAlgorithm getSignAlg() {
		return signAlg;
	}

	public HashAlgorithm getCertHashAlg() {
		return certHashAlg;
	}

	/**
	 * @return true if the certificate belongs to the specified identity
	 */
	public abstract boolean belongsTo(ReloadCertificate certificate, SignerIdentity identity);

	/**
	 * @return the parser used to generate reload certificates
	 */
	public abstract ReloadCertificateParser getCertificateParser();

	public abstract SSLEngine newSSLEngine(OverlayLinkType linkType) throws NoSuchAlgorithmException;

	/**
	 * Tries to create a trust relationship from the peer cert to the trusted
	 * issuer using the available certs. For X509 the method should return the
	 * trusted chain from the peerCert to the Certification Authority.
	 * 
	 * @param peerCert
	 *            The certificate that must be authenticated
	 * @param trustedIssuer
	 *            the issuer certificate that validate the internal trust
	 *            relations
	 * @param availableCerts
	 *            the certificates available to form the trust relationship
	 * @return A list of certificates needed create a trust relation from the
	 *         peerCert to the trustedIssuer, the first element will be the
	 *         peerCert and the last the trustedIssuer
	 * @throws GeneralSecurityException
	 */
	public abstract List<T> getTrustRelationship(T peerCert, T trustedIssuer, List<T> availableCerts) throws GeneralSecurityException;

	/**
	 * @return a signer object used by the local node to sign the
	 *         data. This signer is initialized with the node private key and
	 *         signing algorithms.
	 */
	public Signer newSigner() {
		SignerIdentity identity = SignerIdentity.singleIdIdentity(getCertHashAlg(), getLocalCertificate().getOriginalCertificate());
		Signer signer;
		try {
			signer = new Signer(identity, getSignHashAlg(), getSignAlg());
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
		try {
			signer.initSign(keystore.getPrivateKey());
		} catch (InvalidKeyException e) {
			throw new RuntimeException(e);
		}
		return signer;
	}

	/**
	 * Try to find a trust relation from the specified peerCert to a trusted
	 * issuer of this overlay using the specified certificates to create the
	 * relation. This method must always succeeds if the specified trust
	 * relation is the output of the {@link #getLocalTrustRelationship()} method
	 * of the same instance
	 * 
	 * @throws CertificateException
	 *             if the trusted relation cannot be built
	 */
	@SuppressWarnings("unchecked")
	public void authenticateTrustRelationship(T peerCert, List<T> availableCerts) throws CertificateException {
		List<T> trustedRelation = null;
		List<T> available = new ArrayList<T>(availableCerts);

		available.addAll((List<T>) keystore.getAcceptedIssuers());

		for (Certificate issuerCert : keystore.getAcceptedIssuers()) {
			try {
				trustedRelation = getTrustRelationship(peerCert, (T) issuerCert, available);
				if (trustedRelation != null)
					return;
			} catch (GeneralSecurityException e) {
				// Checked later
			}
		}

		throw new CertificateException("No trusted relation found");
	}

	/**
	 * @return the certificates needed to validate the local node certificate.
	 *         For X509 this method returns the chain to the root certificate of
	 *         the overlay (the enrollment server certificate). The root
	 *         certificate is not included since it is known by all nodes in the
	 *         overlay.
	 */
	@SuppressWarnings("unchecked")
	public List<T> getLocalTrustRelationship() {
		List<T> issuers = (List<T>) keystore.getAcceptedIssuers();

		List<T> relations = null;
		for (T issuer : issuers) {
			try {
				relations = getTrustRelationship((T) keystore.getLocalCertificate().getOriginalCertificate(), issuer, issuers);
				if (relations != null) {
					relations.remove(issuer);
					return relations;
				}
			} catch (GeneralSecurityException e) {
				// Checked later
			}
		}

		throw new RuntimeException("Trust relation for local peer not found");
	}

	/**
	 * @return the local certificate used by the local node for overlay
	 *         operations
	 */
	public ReloadCertificate getLocalCertificate() {
		return keystore.getLocalCertificate();
	}

	/**
	 * Store a new certificate locally, the certificate is validated before it
	 * is stored
	 */
	public void addCertificate(ReloadCertificate cert) {
		keystore.addCertificate(cert);
	}

	public ReloadCertificate getCertificate(NodeID nodeId) {
		return keystore.getCertificate(nodeId);
	}

	public ReloadCertificate getCertificate(SignerIdentity identity) {
		for (ReloadCertificate cert : keystore.getStoredCertificates().values())
			if (belongsTo(cert, identity))
				return cert;

		return null;
	}

	/**
	 * @return all the stored certificates
	 */
	public Map<NodeID, ReloadCertificate> getStoredCertificates() {
		return keystore.getStoredCertificates();
	}

	/**
	 * @return the certificate of the peers accepted as issuer (commonly the
	 *         enrollment peers root certificates)
	 */
	public List<? extends Certificate> getAcceptedIssuers() {
		return keystore.getAcceptedIssuers();
	}

	/**
	 * @return the local node private key
	 */
	public PrivateKey getPrivateKey() {
		return keystore.getPrivateKey();
	}

	/**
	 * @return true if the node is classified as a bad node, false otherwise
	 */
	public boolean isBadNode(NodeID nodeId) {
		return keystore.isBadNode(nodeId);
	}

	/**
	 * Parses the given certificate to a reload certificate
	 * 
	 * @return the parsed certificate
	 * @throws CertificateException
	 *             if the certificate cannot be parsed
	 */
	public ReloadCertificate parseCertificate(T cert, Configuration conf) throws CertificateException {
		ReloadCertificate reloadCert = getCertificateParser().parse(cert);
		if (reloadCert.isSelfSigned() && !conf.isSelfSignedPermitted())
			throw new CertificateException("Self-signed certificates not allowed");

		return reloadCert;
	}
}
