package com.github.reload.crypto;

import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.SecureRandom;
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
import com.github.reload.Components.Component;
import com.github.reload.Components.start;
import com.github.reload.conf.Configuration;
import com.github.reload.net.encoders.secBlock.CertHashSignerIdentityValue;
import com.github.reload.net.encoders.secBlock.HashAlgorithm;
import com.github.reload.net.encoders.secBlock.SignatureAlgorithm;
import com.github.reload.net.encoders.secBlock.SignerIdentity;
import com.github.reload.net.ice.IceCandidate.OverlayLinkType;

/**
 * Crypto helper for X.509 certificates
 * 
 */
@Component(CryptoHelper.COMPNAME)
public class X509CryptoHelper extends CryptoHelper<X509Certificate> {

	@Component(Configuration.COMPNAME)
	private Configuration conf;

	@Component(Keystore.COMPNAME)
	private Keystore<X509Certificate> keystore;

	private SSLContext sslContext;

	private final X509CertificateParser certParser;
	private final HashAlgorithm signHashAlg;
	private final SignatureAlgorithm signAlg;
	private final HashAlgorithm certHashAlg;

	public X509CryptoHelper(HashAlgorithm signHashAlg, SignatureAlgorithm signAlg, HashAlgorithm certHashAlg) {
		certParser = new X509CertificateParser();
		this.signHashAlg = signHashAlg;
		this.signAlg = signAlg;
		this.certHashAlg = certHashAlg;
	}

	@start
	public void init() throws Exception {
		sslContext = SSLContext.getInstance("TLSv1.2");
		sslContext.init(new KeyManager[]{new X509LocalKeyManager()}, new TrustManager[]{new X509LocalTrustManager()}, new SecureRandom());
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
	public List<X509Certificate> getTrustRelationship(X509Certificate peerCert, X509Certificate trustedIssuer, List<? extends X509Certificate> availableCerts) throws CertificateException {
		List<X509Certificate> out = new ArrayList<X509Certificate>();

		X509Certificate certToAuthenticate = peerCert;

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
	protected Keystore<X509Certificate> getKeystore() {
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
		if (identity.getSignerIdentityValue() instanceof CertHashSignerIdentityValue) {
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
	public SSLEngine newSSLEngine(OverlayLinkType linkType) throws NoSuchAlgorithmException {
		return sslContext.createSSLEngine();
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
			return X509CryptoHelper.this.getPrivateKey();
		}

		@Override
		public String[] getServerAliases(String keyType, Principal[] issuers) {
			System.out.println("OK");
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
			return X509CryptoHelper.this.getAcceptedIssuers().toArray(new X509Certificate[0]);
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
