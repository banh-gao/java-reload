package com.github.reload.crypto;

import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509KeyManager;
import javax.security.auth.x500.X500Principal;
import com.github.reload.Components.Component;
import com.github.reload.Components.start;
import com.github.reload.conf.Configuration;
import com.github.reload.net.encoders.header.NodeID;
import com.github.reload.net.encoders.secBlock.CertHashNodeIdSignerIdentityValue;
import com.github.reload.net.encoders.secBlock.CertHashSignerIdentityValue;
import com.github.reload.net.encoders.secBlock.HashAlgorithm;
import com.github.reload.net.encoders.secBlock.SignatureAlgorithm;
import com.github.reload.net.encoders.secBlock.SignerIdentity;
import com.github.reload.net.ice.IceCandidate.OverlayLinkType;

/**
 * Crypto helper for X.509 certificates
 * 
 */
@Component(CryptoHelper.class)
public class X509CryptoHelper extends CryptoHelper {

	@Component
	private Configuration conf;

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

	@start
	public void init() {
		keystore.init(conf);
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
		HashAlgorithm certHashAlg = identity.getSignerIdentityValue().getHashAlgorithm();
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

	@Override
	public SSLEngine getSSLEngine(OverlayLinkType linkType) throws NoSuchAlgorithmException {
		SSLContext ctx = SSLContext.getDefault();
		ctx.init(new KeyManager[]{new X509KeyManager() {
			
			@Override
			public String[] getServerAliases(String keyType, Principal[] issuers) {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public PrivateKey getPrivateKey(String alias) {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public String[] getClientAliases(String keyType, Principal[] issuers) {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public X509Certificate[] getCertificateChain(String alias) {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket) {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public String chooseClientAlias(String[] keyType, Principal[] issuers, Socket socket) {
				// TODO Auto-generated method stub
				return null;
			}
		};
		getKeyManager()}, new TrustManager[]{getTrustManager()}, null);
		sslEngine = ctx.createSSLEngine();
		return sslEngine;
		return null;
	}

	private class LocalKeyManager implements X509KeyManager {

		@Override
		public String chooseClientAlias(String[] keyType, Principal[] issuers, Socket socket) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public X509Certificate[] getCertificateChain(String alias) {
			return getLocalTrustRelationship().toArray(new X509Certificate[]{});
		}

		@Override
		public String[] getClientAliases(String keyType, Principal[] issuers) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public PrivateKey getPrivateKey(String alias) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String[] getServerAliases(String keyType, Principal[] issuers) {
			// TODO Auto-generated method stub
			return null;
		}

	}
}
