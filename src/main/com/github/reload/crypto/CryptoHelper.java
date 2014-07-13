package com.github.reload.crypto;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.CertStoreException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.net.ssl.SSLEngine;
import javax.security.auth.login.Configuration;
import com.github.reload.Context;
import com.github.reload.Context.Component;
import com.github.reload.InitializationException;
import com.github.reload.message.HashAlgorithm;
import com.github.reload.message.NodeID;
import com.github.reload.message.SignatureAlgorithm;
import com.github.reload.message.SignerIdentity;
import com.github.reload.net.ice.IceCandidate.OverlayLinkType;

/**
 * Helper class that provides the cryptographic functionalities for the local
 * node
 * 
 */
public abstract class CryptoHelper implements Component {

	/**
	 * The hashing algorithm of the overlay
	 */
	public static final HashAlgorithm OVERLAY_HASHALG = HashAlgorithm.SHA1;

	/**
	 * Initialize the helper and the associated keystore for the given context
	 * 
	 * @param context
	 * @throws InitializationException
	 */
	@Override
	public abstract void compStart(Context context);

	/**
	 * @return the algorithm used for signatures hashing
	 */
	public abstract HashAlgorithm getSignHashAlg();

	/**
	 * @return the algorithm used for compute the signatures
	 */
	public abstract SignatureAlgorithm getSignAlg();

	/**
	 * @return the algorithm used for certificates hashing
	 */
	public abstract HashAlgorithm getCertHashAlg();

	/**
	 * @return true if the certificate belongs to the specified identity
	 */
	public abstract boolean belongsTo(ReloadCertificate certificate, SignerIdentity identity);

	/**
	 * @return the parser used to generate reload certificates
	 */
	public abstract ReloadCertificateParser getCertificateParser();

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
	 * @throws CertificateException
	 *             if the relationship cannot be created
	 */
	public abstract List<? extends Certificate> getTrustRelationship(Certificate peerCert, Certificate trustedIssuer, List<? extends Certificate> availableCerts) throws CertificateException;

	/**
	 * @return the keystore used by the helper implementation to store
	 *         cryptographic material
	 */
	protected abstract Keystore getKeystore();

	/**
	 * @return a generic signature object used by the local node to sign the
	 *         data. This signer is initialized with the node private key and
	 *         signing algorithms.
	 */
	public Signature newSigner(SignerIdentity signerIdentity) {
		Signature signer;
		try {
			signer = new Signature(signerIdentity, getSignHashAlg(), getSignAlg());
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
		try {
			signer.initSign(getKeystore().getPrivateKey());
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
	public void authenticateTrustRelationship(Certificate peerCert, List<? extends Certificate> availableCerts) throws CertificateException {
		List<? extends Certificate> trustedRelation = null;
		List<Certificate> available = new ArrayList<Certificate>(availableCerts);

		available.addAll(getKeystore().getAcceptedIssuers());

		for (Certificate issuerCert : getKeystore().getAcceptedIssuers()) {
			try {
				trustedRelation = getTrustRelationship(peerCert, issuerCert, available);
				if (trustedRelation != null)
					return;
			} catch (CertificateException e) {
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
		List<? extends Certificate> issuers = getKeystore().getAcceptedIssuers();
		List<? extends Certificate> relations = null;
		for (Certificate issuer : issuers) {
			try {
				relations = getTrustRelationship(getKeystore().getLocalCertificate().getOriginalCertificate(), issuer, issuers);
				if (relations != null) {
					relations.remove(issuer);
					return relations;
				}
			} catch (CertificateException e) {
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
		return getKeystore().getLocalCertificate();
	}

	/**
	 * @return the locally stored certificate that correspond to the specified
	 *         identity or null if no matching certificate was found
	 */
	public ReloadCertificate getCertificate(SignerIdentity identity) {
		return getKeystore().getCertificate(identity);
	}

	/**
	 * Store a new certificate locally, the certificate is validated before it
	 * is stored
	 * 
	 * @throws CertStoreException
	 *             if the certificate storage fails
	 */
	public void addCertificate(Certificate cert) throws CertStoreException {
		getKeystore().addCertificate(cert);
	}

	/**
	 * @return all the stored certificates
	 */
	public Set<ReloadCertificate> getStoredCertificates() {
		return getKeystore().getStoredCertificates();
	}

	/**
	 * @return the certificate of the peers accepted as issuer (commonly the
	 *         enrollment peers root certificates)
	 */
	public List<? extends Certificate> getAcceptedIssuers() {
		return getKeystore().getAcceptedIssuers();
	}

	/**
	 * @return the local node private key
	 */
	public PrivateKey getPrivateKey() {
		return getKeystore().getPrivateKey();
	}

	/**
	 * @return true if the node is classified as a bad node, false otherwise
	 */
	public boolean isBadNode(NodeID nodeId) {
		return getKeystore().isBadNode(nodeId);
	}

	/**
	 * Parses the given certificate to a reload certificate
	 * 
	 * @return the parsed certificate
	 * @throws CertificateException
	 *             if the certificate cannot be parsed
	 */
	public ReloadCertificate parseCertificate(Certificate cert, Configuration conf) throws CertificateException {
		ReloadCertificate reloadCert = getCertificateParser().parse(cert);
		if (reloadCert.isSelfSigned() && !conf.isSelfSignedPermitted())
			throw new CertificateException("Self-signed certificates not allowed");

		return reloadCert;
	}

	public SSLEngine getClientSSLEngine(OverlayLinkType linkType) {
		// TODO Auto-generated method stub
		return null;
	}

	public SSLEngine getServerSSLEngine(OverlayLinkType linkType) {
		// TODO Auto-generated method stub
		return null;
	}
}