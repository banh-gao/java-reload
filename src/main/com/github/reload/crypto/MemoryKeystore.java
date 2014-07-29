package com.github.reload.crypto;

import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.github.reload.components.ComponentsRepository.Component;
import com.github.reload.conf.Configuration;
import com.github.reload.net.encoders.header.NodeID;

/**
 * Keystore that stored initialization values and running crypto material into
 * memory
 */
@Component(Keystore.class)
public class MemoryKeystore<T extends Certificate> implements Keystore<T> {

	@Component
	private Configuration conf;

	@Component
	protected CryptoHelper<T> cryptoHelper;

	private static PrivateKey privateKey;

	private static ReloadCertificate localCert;
	private static Map<NodeID, ReloadCertificate> storedCerts;

	public static void init(ReloadCertificate localCert, PrivateKey privateKey, Map<NodeID, ReloadCertificate> storedCerts) {
		// FIXME: derive from context
		MemoryKeystore.privateKey = privateKey;
		MemoryKeystore.localCert = localCert;
		MemoryKeystore.storedCerts = new HashMap<NodeID, ReloadCertificate>(storedCerts);
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

	@SuppressWarnings("unchecked")
	@Override
	public List<? extends T> getAcceptedIssuers() {
		return (List<? extends T>) conf.getRootCerts();
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
