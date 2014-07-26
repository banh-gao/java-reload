package com.github.reload.crypto;

import java.security.PrivateKey;
import java.security.cert.CertStoreException;
import java.security.cert.Certificate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.github.reload.Components.Component;
import com.github.reload.conf.Configuration;
import com.github.reload.net.encoders.header.NodeID;

/**
 * Keystore that stored initialization values and running crypto material into
 * memory
 */
@Component(Keystore.COMPNAME)
public class MemoryKeystore<T extends Certificate> implements Keystore<T> {

	@Component(Configuration.COMPNAME)
	private Configuration conf;

	@Component(CryptoHelper.COMPNAME)
	protected CryptoHelper<T> cryptoHelper;

	private final PrivateKey privateKey;

	private final T localCert;
	private final Map<NodeID, T> storedCerts;

	public MemoryKeystore(T localCert, PrivateKey privateKey, Map<NodeID, T> storedCerts) {
		this.privateKey = privateKey;
		this.localCert = localCert;
		this.storedCerts = new HashMap<NodeID, T>(storedCerts);
	}

	@Override
	public T getLocalCertificate() {
		return localCert;
	}

	@Override
	public void addCertificate(NodeID nodeId, T cert) throws CertStoreException {
		storedCerts.put(nodeId, cert);
	}

	@Override
	public void removeCertificate(T cert) {
		if (localCert.equals(cert))
			return;

		storedCerts.remove(cert);
	}

	@Override
	public Map<NodeID, T> getStoredCertificates() {
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
	public T getCertificate(NodeID node) {
		return storedCerts.get(node);
	}

	@Override
	public boolean isBadNode(NodeID nodeId) {
		return (conf.getBadNodes().contains(nodeId));
	}
}
