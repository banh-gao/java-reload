package com.github.reload.crypto;

import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import javax.security.auth.x500.X500Principal;
import com.github.reload.Configuration;
import com.github.reload.message.CertHashNodeIdSignerIdentityValue;
import com.github.reload.message.CertHashSignerIdentityValue;
import com.github.reload.message.HashAlgorithm;
import com.github.reload.message.NodeID;
import com.github.reload.message.SignatureAlgorithm;
import com.github.reload.message.SignerIdentity;

/**
 * Crypto helper for X.509 certificates
 * 
 */
public class X509CryptoHelper extends CryptoHelper {

	private final Keystore keystore;
	private final X509CertificateParser certParser;
	private final HashAlgorithm signHashAlg;
	private final SignatureAlgorithm signAlg;
	private final HashAlgorithm certHashAlg;

	public X509CryptoHelper(Keystore keystore, HashAlgorithm signHashAlg, SignatureAlgorithm signAlg, HashAlgorithm certHashAlg) {
		this.keystore = keystore;
		certParser = new X509CertificateParser();
		this.signHashAlg = signHashAlg;
		this.signAlg = signAlg;
		this.certHashAlg = certHashAlg;
	}

	@Override
	public void init(Configuration conf) throws InitializationException {
		keystore.init(context);
	}

	@Override
	public HashAlgorithm getSignHashAlg() {
		return signHashAlg;
	}

	@Override
	public SignatureAlgorithm getSignAlg() {
		return signAlg;
	}

	@Override
	public HashAlgorithm getCertHashAlg() {
		return certHashAlg;
	}

	@Override
	public List<X509Certificate> getTrustRelationship(Certificate peerCert, Certificate trustedIssuer, List<? extends Certificate> availableCerts) throws CertificateException {
		if (!(peerCert instanceof X509Certificate))
			throw new IllegalArgumentException("Invalid X509 certificate");

		if (!(trustedIssuer instanceof X509Certificate))
			throw new IllegalArgumentException("Invalid X509 certificate");

		List<X509Certificate> out = new ArrayList<X509Certificate>();

		X509Certificate certToAuthenticate = (X509Certificate) peerCert;

		while (true) {
			X500Principal issuer = certToAuthenticate.getIssuerX500Principal();
			X509Certificate issuerCert = X509CertificateParser.getMatchingCertificate(issuer, availableCerts);

			if (issuerCert == null)
				throw new CertificateException("Certificate not found for issuer: [" + issuer + "]");

			out.add(certToAuthenticate);

			if (trustedIssuer.equals(issuerCert)) {
				break;
			}

			certToAuthenticate = issuerCert;
		}

		out.add((X509Certificate) trustedIssuer);
		return out;
	}

	@Override
	protected Keystore getKeystore() {
		return keystore;
	}

	/**
	 * @return true if the specified certificate belongs to the specified signer
	 *         identity
	 * @throws CertificateException
	 */
	@Override
	public boolean belongsTo(ReloadCertificate certificate, SignerIdentity identity) {
		HashAlgorithm certHashAlg = identity.getSignerIdentityValue().getCertHashAlg();
		SignerIdentity computedIdentity = null;
		if (identity.getSignerIdentityValue() instanceof CertHashNodeIdSignerIdentityValue) {
			for (NodeID nodeId : certificate.getNodeIds()) {
				computedIdentity = SignerIdentity.multipleIdIdentity(certHashAlg, certificate.getOriginalCertificate(), nodeId);
				if (computedIdentity.equals(identity))
					return true;
			}
		} else if (identity.getSignerIdentityValue() instanceof CertHashSignerIdentityValue) {
			computedIdentity = SignerIdentity.singleIdIdentity(certHashAlg, certificate.getOriginalCertificate());
			return computedIdentity.equals(identity);
		}
		return false;
	}

	@Override
	public ReloadCertificateParser getCertificateParser() {
		return certParser;
	}
}
