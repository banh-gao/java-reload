package com.github.reload;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Collections;
import com.github.reload.Components.Component;
import com.github.reload.crypto.CryptoHelper;
import com.github.reload.crypto.MemoryKeystore;
import com.github.reload.crypto.ReloadCertificate;
import com.github.reload.crypto.X509CryptoHelper;
import com.github.reload.net.encoders.header.NodeID;
import com.github.reload.net.encoders.secBlock.GenericCertificate.CertificateType;
import com.github.reload.net.encoders.secBlock.HashAlgorithm;
import com.github.reload.net.encoders.secBlock.SignatureAlgorithm;

@Component(Bootstrap.COMPNAME)
public class TestBootstrap extends Bootstrap {

	public static HashAlgorithm TEST_HASH = HashAlgorithm.SHA1;
	public static SignatureAlgorithm TEST_SIGN = SignatureAlgorithm.RSA;
	public static NodeID TEST_NODEID = NodeID.valueOf("f16a536ca4028b661fcb864a075f3871");

	public static InetSocketAddress SERVER_ADDR;

	public static X509Certificate CA_CERT;
	public static ReloadCertificate TEST_CERT;
	public static PrivateKey TEST_KEY;

	public static CryptoHelper<X509Certificate> TEST_CRYPTO;

	public TestBootstrap() throws Exception {
		TEST_CRYPTO = new X509CryptoHelper(TEST_HASH, TEST_SIGN, TEST_HASH);
		TEST_CERT = TEST_CRYPTO.getCertificateParser().parse(loadLocalCert("testCert.der"));
		TEST_KEY = loadPrivateKey("testKey.der", SignatureAlgorithm.RSA);
	}

	@Override
	protected byte[] getJoinData() {
		return new byte[0];
	}

	@Override
	protected CertificateType getCertificateType() {
		return CertificateType.X509;
	}

	@Override
	public boolean equals(Object obj) {
		return false;
	}

	@Override
	public int hashCode() {
		return 0;
	}

	@Override
	protected void registerComponents() {
		Components.register(TEST_CRYPTO);
		Components.register(new MemoryKeystore<X509Certificate>(TEST_CERT, TEST_KEY, Collections.singletonMap(TEST_NODEID, TEST_CERT)));
	}

	public static Certificate loadLocalCert(String localCertPath) throws FileNotFoundException, CertificateException {
		if (localCertPath == null || !new File(localCertPath).exists())
			throw new FileNotFoundException("Overlay certificate file not found at " + localCertPath);

		CertificateFactory certFactory;
		try {
			certFactory = CertificateFactory.getInstance("X.509");
			File overlayCertFile = new File(localCertPath);
			InputStream certStream = new FileInputStream(overlayCertFile);
			Certificate cert = certFactory.generateCertificate(certStream);
			certStream.close();
			return cert;
		} catch (CertificateException | IOException e) {
			throw new CertificateException(e);
		}
	}

	private static PrivateKey loadPrivateKey(String privateKeyPath, SignatureAlgorithm keyAlg) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
		if (privateKeyPath == null || !new File(privateKeyPath).exists())
			throw new FileNotFoundException("Private key file not found at " + privateKeyPath);

		File file = new File(privateKeyPath);
		byte[] privKeyBytes = new byte[(int) file.length()];
		InputStream in = new FileInputStream(file);
		in.read(privKeyBytes);
		in.close();
		KeyFactory keyFactory = KeyFactory.getInstance(keyAlg.toString());
		KeySpec ks = new PKCS8EncodedKeySpec(privKeyBytes);
		return keyFactory.generatePrivate(ks);
	}
}