package com.github.reload.crypto;

import java.net.Socket;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509ExtendedKeyManager;
import javax.net.ssl.X509ExtendedTrustManager;
import javax.security.auth.x500.X500Principal;
import com.github.reload.conf.Configuration;
import com.github.reload.net.codecs.secBlock.HashAlgorithm;
import com.github.reload.net.codecs.secBlock.SignatureAlgorithm;
import com.github.reload.net.ice.HostCandidate.OverlayLinkType;

/**
 * Crypto helper for X.509 certificates
 * 
 */
public class X509CryptoHelper extends CryptoHelper {

	private final Configuration conf;
	private final Keystore keystore;
	private final SSLContext sslContext;

	public X509CryptoHelper(Keystore keystore, Configuration conf, HashAlgorithm signHashAlg, SignatureAlgorithm signAlg, HashAlgorithm certHashAlg) {
		super(keystore, conf, signHashAlg, signAlg, certHashAlg);

		this.keystore = keystore;
		this.conf = conf;

		try {
			sslContext = SSLContext.getInstance("TLS");
			sslContext.init(new KeyManager[]{new X509LocalKeyManager()}, new TrustManager[]{new X509LocalTrustManager()}, new SecureRandom());
		} catch (KeyManagementException | NoSuchAlgorithmException e) {
			throw new IllegalArgumentException(e);
		}
	}

	@Override
	public List<X509Certificate> getTrustRelationship(Certificate peerCert, Certificate trustedIssuer, List<? extends Certificate> availableCerts) throws GeneralSecurityException {
		List<X509Certificate> out = new ArrayList<X509Certificate>();

		X509Certificate certToAuthenticate = (X509Certificate) peerCert;

		while (true) {
			X500Principal issuer = certToAuthenticate.getIssuerX500Principal();
			X509Certificate issuerCert = X509CertificateParser.getMatchingCertificate(issuer, availableCerts);

			if (issuerCert == null)
				throw new CertificateException("Certificate not found for issuer: [" + issuer + "]");

			peerCert.verify(issuerCert.getPublicKey());

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
	public ReloadCertificate toReloadCertificate(Certificate cert) throws CertificateException {
		return X509CertificateParser.parse((X509Certificate) cert);
	}

	@Override
	public SSLEngine newSSLEngine(OverlayLinkType linkType) throws NoSuchAlgorithmException {
		SSLEngine e = sslContext.createSSLEngine();
		e.setEnabledCipherSuites(new String[]{"SSL_RSA_WITH_RC4_128_MD5"});
		return e;
	}

	/**
	 * Key manager that uses the cryptographic material related to the local
	 * node
	 * 
	 */
	public class X509LocalKeyManager extends X509ExtendedKeyManager {

		private static final String LOCAL_ALIAS = "LOCAL_ALIAS";

		@Override
		public String chooseClientAlias(String[] keyType, Principal[] issuers, Socket socket) {
			return LOCAL_ALIAS;
		}

		@Override
		public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket) {
			return LOCAL_ALIAS;
		}

		@Override
		public String chooseEngineServerAlias(String keyType, Principal[] issuers, SSLEngine engine) {
			return chooseServerAlias(keyType, issuers, null);
		}

		@Override
		public String chooseEngineClientAlias(String[] keyType, Principal[] issuers, SSLEngine engine) {
			return chooseClientAlias(keyType, issuers, null);
		}

		@Override
		public X509Certificate[] getCertificateChain(String alias) {
			return getLocalTrustRelationship().toArray(new X509Certificate[0]);
		}

		@Override
		public String[] getClientAliases(String keyType, Principal[] issuers) {
			return new String[]{LOCAL_ALIAS};
		}

		@Override
		public PrivateKey getPrivateKey(String alias) {
			return keystore.getLocalKey();
		}

		@Override
		public String[] getServerAliases(String keyType, Principal[] issuers) {
			return new String[]{LOCAL_ALIAS};
		}
	}

	/**
	 * Trust manager that uses the cryptographic material related to the local
	 * node
	 * 
	 */
	public class X509LocalTrustManager extends X509ExtendedTrustManager {

		@Override
		public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
			X509Certificate remoteCert = chain[0];
			// Authenticate client certificate using overlay CA
			authenticateTrustRelationship(remoteCert, Arrays.asList(chain));
		}

		@Override
		public X509Certificate[] getAcceptedIssuers() {
			return conf.get(Configuration.ROOT_CERTS).toArray(new X509Certificate[0]);
		}

		@Override
		public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
			X509Certificate remoteCert = chain[0];
			// Authenticate server certificate using overlay CA
			authenticateTrustRelationship(remoteCert, Arrays.asList(chain));
		}

		@Override
		public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket) throws CertificateException {
			checkClientTrusted(chain, authType);
		}

		@Override
		public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket) throws CertificateException {
			checkServerTrusted(chain, authType);
		}

		@Override
		public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine) throws CertificateException {
			checkClientTrusted(chain, authType);
		}

		@Override
		public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine engine) throws CertificateException {
			checkServerTrusted(chain, authType);
		}
	}
}
