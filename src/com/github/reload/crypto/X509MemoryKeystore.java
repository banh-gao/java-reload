package com.github.reload.crypto;

import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.cert.CertStoreException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Iterator;
import java.util.Set;
import com.github.reload.ReloadOverlay;
import com.github.reload.message.SignerIdentity;

/**
 * Memory keystore for x.509 certificates
 * 
 */
public class X509MemoryKeystore extends MemoryKeystore {

	private static final Logger logger = Logger.getLogger(ReloadOverlay.class);

	public X509MemoryKeystore(ReloadCertificate localCert, PrivateKey privateKey, Set<ReloadCertificate> storedCerts) {
		super(localCert, privateKey, storedCerts);
	}

	@Override
	public ReloadCertificate getCertificate(SignerIdentity identity) {
		ReloadCertificate reloadCert = getLocalCertificate();
		if (context.getCryptoHelper().belongsTo(reloadCert, identity)) {
			try {
				reloadCert.checkValidity();
			} catch (CertificateException e) {
				removeCertificate(reloadCert);
				logger.log(Priority.WARN, "Removed expired certificate: " + reloadCert.toString());
				return getCertificate(identity);
			}
			return reloadCert;
		}
		Iterator<ReloadCertificate> i = getStoredCertificates().iterator();

		while (i.hasNext()) {
			ReloadCertificate tmpReloadCert = i.next();
			if (context.getCryptoHelper().belongsTo(tmpReloadCert, identity)) {
				reloadCert = tmpReloadCert;

				try {
					((X509Certificate) reloadCert.getOriginalCertificate()).checkValidity();
				} catch (CertificateException e) {
					i.remove();
					logger.log(Priority.WARN, "Void certificate removed");
				}

				return reloadCert;
			}
		}
		return null;
	}

	@Override
	public void addCertificate(Certificate cert) throws CertStoreException {
		if (!(cert instanceof X509Certificate))
			throw new IllegalArgumentException("Invalid X509 certificate");
		X509Certificate x509Cert = (X509Certificate) cert;
		Certificate trustedIssuer = X509CertificateParser.getMatchingCertificate(x509Cert.getIssuerX500Principal(), getAcceptedIssuers());
		if (trustedIssuer == null)
			throw new CertStoreException(new CertificateException("Untrusted certificate issuer"));

		try {
			x509Cert.verify(trustedIssuer.getPublicKey());
			x509Cert.checkValidity();

		} catch (GeneralSecurityException e) {
			throw new CertStoreException(e);
		}

		super.addCertificate(x509Cert);
	}
}
