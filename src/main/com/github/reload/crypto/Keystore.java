package com.github.reload.crypto;

import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.util.List;
import java.util.Map;
import com.github.reload.net.encoders.header.NodeID;

/**
 * Defines the methods to store and retrieve cryptographic material needed for
 * the protocol
 * 
 */
public interface Keystore<T extends Certificate> {

	/**
	 * @return the local certificate used by the local node for overlay
	 *         operations
	 */
	public ReloadCertificate getLocalCertificate();

	/**
	 * @return the locally stored certificate that correspond to the specified
	 *         node-id or null if no matching certificate was found
	 */
	public ReloadCertificate getCertificate(NodeID node);

	/**
	 * Store a new certificate locally, the certificate is validated before it
	 * is stored
	 */
	public void addCertificate(ReloadCertificate cert);

	/**
	 * Remove a locally stored certificate
	 */
	public void removeCertificate(NodeID certOwner);

	/**
	 * @return all the stored certificates
	 */
	public Map<NodeID, ReloadCertificate> getStoredCertificates();

	/**
	 * @return the certificate of the peers accepted as issuer (commonly the
	 *         enrollment peers root certificates)
	 */
	public List<? extends T> getAcceptedIssuers();

	/**
	 * @return the local node private key
	 */
	public PrivateKey getPrivateKey();

	/**
	 * @return true if the specified node is classified as not valid
	 */
	public boolean isBadNode(NodeID nodeId);
}
