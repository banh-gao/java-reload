package com.github.reload.crypto;

import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.net.ssl.SSLEngine;
import com.github.reload.conf.Configuration;
import com.github.reload.net.encoders.secBlock.HashAlgorithm;
import com.github.reload.net.encoders.secBlock.SignatureAlgorithm;
import com.github.reload.net.encoders.secBlock.SignerIdentity;
import com.github.reload.net.ice.HostCandidate.OverlayLinkType;

/**
 * Helper class that provides the cryptographic functionalities for the local
 * node
 * 
 */
public abstract class CryptoHelper {

	/**
	 * The hashing algorithm of the overlay
	 */
	public static final HashAlgorithm OVERLAY_HASHALG = HashAlgorithm.SHA1;

	@Inject
	Keystore keystore;

	@Inject
	Configuration conf;

	private HashAlgorithm signHashAlg;
	private SignatureAlgorithm signAlg;
	private HashAlgorithm certHashAlg;

	public CryptoHelper(HashAlgorithm signHashAlg, SignatureAlgorithm signAlg, HashAlgorithm certHashAlg) {
		this.signHashAlg = signHashAlg;
		this.signAlg = signAlg;
		this.certHashAlg = certHashAlg;
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
	 * @return Tries to convert the given certificate into a reload certificate
	 */
	public abstract ReloadCertificate toReloadCertificate(Certificate cert) throws CertificateException;

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
	public abstract List<? extends Certificate> getTrustRelationship(Certificate peerCert, Certificate trustedIssuer, List<? extends Certificate> availableCerts) throws GeneralSecurityException;

	/**
	 * @return a signer object used by the local node to sign the
	 *         data. This signer is initialized with the node private key and
	 *         signing algorithms.
	 */
	public Signer newSigner() {
		SignerIdentity identity = SignerIdentity.singleIdIdentity(getCertHashAlg(), keystore.getLocalCert().getOriginalCertificate());
		Signer signer;
		try {
			signer = new Signer(identity, keystore.getLocalKey(), getSignHashAlg(), getSignAlg());
		} catch (NoSuchAlgorithmException | InvalidKeyException e) {
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
	public void authenticateTrustRelationship(Certificate peerCert, List<? extends Certificate> availableCerts) throws CertificateException {
		List<? extends Certificate> trustedRelation = null;
		List<Certificate> available = new ArrayList<Certificate>(availableCerts);

		available.addAll(conf.get(Configuration.ROOT_CERTS));

		for (Certificate issuerCert : conf.get(Configuration.ROOT_CERTS)) {
			try {
				trustedRelation = getTrustRelationship(peerCert, issuerCert, available);
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
	public List<? extends Certificate> getLocalTrustRelationship() {
		List<? extends Certificate> issuers = conf.get(Configuration.ROOT_CERTS);

		List<? extends Certificate> relations = null;
		for (Certificate issuer : issuers) {
			try {
				relations = getTrustRelationship(keystore.getLocalCert().getOriginalCertificate(), issuer, issuers);
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
}
