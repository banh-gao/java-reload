package com.github.reload;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
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
import com.github.reload.components.ComponentsRepository;
import com.github.reload.components.ComponentsRepository.Component;
import com.github.reload.conf.Configuration;
import com.github.reload.crypto.ReloadCertificate;
import com.github.reload.crypto.X509CertificateParser;
import com.github.reload.net.encoders.secBlock.SignatureAlgorithm;

@Component(Bootstrap.class)
public class TestBootstrap extends Bootstrap {

	public TestBootstrap(Configuration conf) throws Exception {
		super(conf);
	}

	@Override
	protected byte[] getJoinData() {
		return "JOIN REQ".getBytes();
	}

	@Override
	protected void registerComponents() {
		ComponentsRepository.register(TestPlugin.class);
	}

	public static ReloadCertificate loadCert(String certPath) throws CertificateException, FileNotFoundException {
		return X509CertificateParser.parse((X509Certificate) loadLocalCert(certPath));
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

	public static PrivateKey loadPrivateKey(String privateKeyPath, SignatureAlgorithm keyAlg) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
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