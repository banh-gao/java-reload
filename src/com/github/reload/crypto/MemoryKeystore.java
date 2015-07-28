package com.github.reload.crypto;

import java.security.PrivateKey;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import com.github.reload.net.encoders.header.NodeID;
import com.google.common.base.Optional;

/**
 * Keystore that stored initialization values and running crypto material into
 * memory
 */
public class MemoryKeystore implements Keystore {

	private final Map<NodeID, ReloadCertificate> storedCerts = new HashMap<NodeID, ReloadCertificate>();
	private final ReloadCertificate localCert;
	private final PrivateKey localKey;

	public MemoryKeystore(ReloadCertificate localCert, PrivateKey localKey) {
		this.localCert = localCert;
		this.localKey = localKey;
		addCertificate(localCert);
	}

	@Override
	public void addCertificate(ReloadCertificate cert) {
		storedCerts.put(cert.getNodeId(), cert);
	}

	@Override
	public void removeCertificate(NodeID certOwner) {
		storedCerts.remove(certOwner);
	}

	@Override
	public Optional<ReloadCertificate> getCertificate(NodeID certOwner) {
		return Optional.fromNullable(storedCerts.get(certOwner));
	}

	@Override
	public Map<NodeID, ReloadCertificate> getStoredCertificates() {
		return Collections.unmodifiableMap(storedCerts);
	}

	@Override
	public ReloadCertificate getLocalCert() {
		return localCert;
	}

	@Override
	public PrivateKey getLocalKey() {
		return localKey;
	}
}
