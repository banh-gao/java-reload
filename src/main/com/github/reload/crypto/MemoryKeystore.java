package com.github.reload.crypto;

import java.security.PrivateKey;
import java.security.cert.CertStoreException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.List;
import java.util.Set;
import com.github.reload.Configuration;
import com.github.reload.net.encoders.header.NodeID;

/**
 * Keystore that stored initialization values and running crypto material into
 * memory
 * 
 * @param <T>
 */
public abstract class MemoryKeystore implements Keystore {

	protected Configuration conf;
	private final PrivateKey privateKey;

	private final ReloadCertificate localCert;
	private final Set<ReloadCertificate> storedCerts;

	public MemoryKeystore(ReloadCertificate localCert, PrivateKey privateKey, Set<ReloadCertificate> storedCerts) {
		this.privateKey = privateKey;
		this.localCert = localCert;
		this.storedCerts = storedCerts;
	}

	@Override
	public void init(Configuration conf) throws InitializationException {
		context = context;
	}

	@Override
	public boolean isBadNode(NodeID nodeId) {
		return (context.getConfiguration().getBadNodes().contains(nodeId));
	}

	@Override
	public ReloadCertificate getLocalCertificate() {
		return localCert;
	}

	@Override
	public void addCertificate(Certificate cert) throws CertStoreException {
		try {
			ReloadCertificate reloadCert = context.getCryptoHelper().parseCertificate(cert, context.getConfiguration());
			storedCerts.add(reloadCert);
		} catch (CertificateException e) {
			throw new CertStoreException(e);
		}
	}

	@Override
	public void removeCertificate(ReloadCertificate cert) {
		if (localCert.equals(cert))
			return;

		storedCerts.remove(cert);
	}

	@Override
	public Set<ReloadCertificate> getStoredCertificates() {
		return storedCerts;
	}

	@Override
	public List<? extends Certificate> getAcceptedIssuers() {
		return context.getConfiguration().getRootCerts();
	}

	@Override
	public PrivateKey getPrivateKey() {
		return privateKey;
	}
}
