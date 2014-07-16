package com.github.reload.crypto;

import java.security.PrivateKey;
import java.security.cert.CertStoreException;
import java.security.cert.Certificate;
import java.util.List;
import java.util.Set;
import com.github.reload.Configuration;
import com.github.reload.net.encoders.header.NodeID;
import com.github.reload.net.encoders.secBlock.SignerIdentity;

/**
 * Defines the methods to store and retrieve cryptographic material needed for
 * the protocol
 * 
 */
public interface Keystore {

	public void init(Configuration conf) throws InitializationException;

	/**
	 * @return the local certificate used by the local node for overlay
	 *         operations
	 */
	public ReloadCertificate getLocalCertificate();

	/**
	 * @return the locally stored certificate that correspond to the specified
	 *         identity or null if no matching certificate was found
	 */
	public ReloadCertificate getCertificate(SignerIdentity identity);

	/**
	 * Store a new certificate locally, the certificate is validated before it
	 * is stored
	 * 
	 * @throws CertStoreException
	 *             if the certificate storage fails
	 * @throws CertificateException
	 */
	public void addCertificate(Certificate cert) throws CertStoreException;

	/**
	 * Remove a locally stored certificate
	 */
	public void removeCertificate(ReloadCertificate cert);

	/**
	 * @return all the stored certificates
	 */
	public Set<ReloadCertificate> getStoredCertificates();

	/**
	 * @return the certificate of the peers accepted as issuer (commonly the
	 *         enrollment peers root certificates)
	 */
	public List<? extends Certificate> getAcceptedIssuers();

	/**
	 * @return the local node private key
	 */
	public PrivateKey getPrivateKey();

	/**
	 * @return true if the specified node is classified as not valid
	 */
	public boolean isBadNode(NodeID nodeId);
}
