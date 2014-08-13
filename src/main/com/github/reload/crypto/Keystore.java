package com.github.reload.crypto;

import java.util.Map;
import com.github.reload.net.encoders.header.NodeID;
import com.github.reload.net.encoders.secBlock.CertHashSignerIdentityValue;
import com.github.reload.net.encoders.secBlock.HashAlgorithm;
import com.github.reload.net.encoders.secBlock.SignerIdentity;
import com.google.common.base.Optional;

/**
 * Defines the methods to store and retrieve cryptographic material needed for
 * the protocol
 * 
 */
public abstract class Keystore {

	/**
	 * @return the locally stored certificate that correspond to the specified
	 *         node-id
	 */
	public abstract Optional<ReloadCertificate> getCertificate(NodeID node);

	/**
	 * Store a new certificate locally, the certificate is validated before it
	 * is stored
	 */
	public abstract void addCertificate(ReloadCertificate cert);

	/**
	 * Remove a locally stored certificate
	 */
	public abstract void removeCertificate(NodeID certOwner);

	/**
	 * @return all the stored certificates
	 */
	public abstract Map<NodeID, ReloadCertificate> getStoredCertificates();

	/**
	 * @return the locally stored certificate that correspond to the specified
	 *         node-id
	 */
	public Optional<ReloadCertificate> getCertificate(SignerIdentity identity) {
		for (ReloadCertificate cert : getStoredCertificates().values())
			if (belongsTo(cert, identity))
				return Optional.of(cert);

		return Optional.absent();
	}

	private boolean belongsTo(ReloadCertificate certificate, SignerIdentity identity) {
		HashAlgorithm certHashAlg = identity.getSignerIdentityValue().getHashAlgorithm();
		SignerIdentity computedIdentity = null;
		if (identity.getSignerIdentityValue() instanceof CertHashSignerIdentityValue) {
			computedIdentity = SignerIdentity.singleIdIdentity(certHashAlg, certificate.getOriginalCertificate());
			return computedIdentity.equals(identity);
		}
		return false;
	}
}
