package com.github.reload.crypto;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import com.github.reload.net.encoders.header.NodeID;
import com.google.common.base.Optional;

/**
 * Keystore that stored initialization values and running crypto material into
 * memory
 */
public class MemoryKeystore extends Keystore {

	private final Map<NodeID, ReloadCertificate> storedCerts = new HashMap<NodeID, ReloadCertificate>();

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
}
