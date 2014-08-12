package com.github.reload.crypto;

import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.github.reload.Bootstrap;
import com.github.reload.components.ComponentsContext.CompStart;
import com.github.reload.components.ComponentsRepository.Component;
import com.github.reload.conf.Configuration;
import com.github.reload.net.encoders.header.NodeID;

/**
 * Keystore that stored initialization values and running crypto material into
 * memory
 */
@Component(value = Keystore.class, priority = 1)
public class MemoryKeystore implements Keystore {

	@Component
	private Configuration conf;

	@Component
	private Bootstrap bootstrap;

	private final Map<NodeID, ReloadCertificate> storedCerts = new HashMap<NodeID, ReloadCertificate>();

	private PrivateKey privateKey;
	private ReloadCertificate localCert;

	@CompStart
	public void start() {
		privateKey = bootstrap.getLocalKey();
		localCert = bootstrap.getLocalCert();
		addCertificate(localCert);
	}

	@Override
	public ReloadCertificate getLocalCertificate() {
		return localCert;
	}

	@Override
	public void addCertificate(ReloadCertificate cert) {
		storedCerts.put(cert.getNodeId(), cert);
	}

	@Override
	public void removeCertificate(NodeID certOwner) {
		if (localCert.getNodeId().equals(certOwner))
			return;

		storedCerts.remove(certOwner);
	}

	@Override
	public Map<NodeID, ReloadCertificate> getStoredCertificates() {
		return Collections.unmodifiableMap(storedCerts);
	}

	@Override
	public List<? extends Certificate> getAcceptedIssuers() {
		return conf.getRootCerts();
	}

	@Override
	public PrivateKey getPrivateKey() {
		return privateKey;
	}

	@Override
	public ReloadCertificate getCertificate(NodeID certOwner) {
		return storedCerts.get(certOwner);
	}

	@Override
	public boolean isBadNode(NodeID nodeId) {
		return (conf.getBadNodes().contains(nodeId));
	}
}
