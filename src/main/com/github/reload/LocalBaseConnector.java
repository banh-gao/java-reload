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
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.HashSet;
import java.util.Set;
import javax.naming.ConfigurationException;
import com.github.reload.Components.Component;
import com.github.reload.conf.Configuration;
import com.github.reload.crypto.CryptoHelper;
import com.github.reload.crypto.ReloadCertificate;
import com.github.reload.net.encoders.secBlock.GenericCertificate.CertificateType;
import com.github.reload.net.encoders.secBlock.SignatureAlgorithm;

public abstract class LocalBaseConnector extends Bootstrap {

	@Component
	private CryptoHelper cryptoHelper;

	private ReloadCertificate localCert;
	private PrivateKey privateKey;
	private Set<ReloadCertificate> storedCerts;

	public LocalBaseConnector(Configuration conf, Bootstrap connector) {
		super(conf, connector);
		if (connector instanceof LocalBaseConnector) {
			LocalBaseConnector tb = (LocalBaseConnector) connector;
			localCert = tb.getLocalCert();
			privateKey = tb.getPrivateKey();
			storedCerts = tb.getStoredCerts();
		}
	}

	public void setLocalCert(String localCertPath) throws FileNotFoundException, CertificateException {
		loadLocalCert(localCertPath);
	}

	public void setPrivateKey(String privateKeyPath, SignatureAlgorithm keyAlg) throws NoSuchAlgorithmException, InvalidKeySpecException, IOException {
		loadPrivateKey(privateKeyPath, keyAlg);
	}

	public void setCertsDir(String storedCertsDir) throws FileNotFoundException, ConfigurationException {
		loadCerts(storedCertsDir);
	}

	public void loadLocalCert(String localCertPath) throws FileNotFoundException, CertificateException {
		if (localCertPath == null || !new File(localCertPath).exists())
			throw new FileNotFoundException("Overlay certificate file not found at " + localCertPath);

		CertificateFactory certFactory;
		try {
			certFactory = CertificateFactory.getInstance(getCertificateType().getType());
			File overlayCertFile = new File(localCertPath);
			InputStream certStream = new FileInputStream(overlayCertFile);
			Certificate cert = certFactory.generateCertificate(certStream);
			localCert = cryptoHelper.parseCertificate(cert, getConfiguration());
			certStream.close();
		} catch (CertificateException e) {
			throw new CertificateException(e);
		} catch (IOException e) {
			// Ignored
		}
	}

	private void loadPrivateKey(String privateKeyPath, SignatureAlgorithm keyAlg) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
		if (privateKeyPath == null || !new File(privateKeyPath).exists())
			throw new FileNotFoundException("Private key file not found at " + privateKeyPath);

		File file = new File(privateKeyPath);
		byte[] privKeyBytes = new byte[(int) file.length()];
		InputStream in = new FileInputStream(file);
		in.read(privKeyBytes);
		in.close();
		KeyFactory keyFactory = KeyFactory.getInstance(keyAlg.toString());
		KeySpec ks = new PKCS8EncodedKeySpec(privKeyBytes);
		privateKey = keyFactory.generatePrivate(ks);
	}

	private void loadCerts(String storedCertsDir) throws FileNotFoundException, ConfigurationException {
		if (storedCertsDir == null || !new File(storedCertsDir).isDirectory())
			throw new FileNotFoundException("Certificates directory not found at " + storedCertsDir);

		CertificateFactory certFactory;
		try {
			certFactory = CertificateFactory.getInstance(getCertificateType().getType());
		} catch (CertificateException e) {
			throw new ConfigurationException(e);
		}
		File certsDir = new File(storedCertsDir);
		File[] certs = certsDir.listFiles();

		storedCerts = new HashSet<ReloadCertificate>();

		for (File cert : certs) {
			try {
				if (!cert.getPath().endsWith(".der")) {
					continue;
				}

				InputStream certStream = new FileInputStream(cert);
				Certificate c = certFactory.generateCertificate(certStream);
				certStream.close();
				storedCerts.add(cryptoHelper.parseCertificate(c, getConfiguration()));
			} catch (FileNotFoundException e) {
				throw new ConfigurationException(e);
			} catch (CertificateException e) {
				throw new ConfigurationException(e);
			} catch (IOException e) {
				// Ignored
			}
		}
	}

	@Override
	public byte[] getJoinData() {
		return new byte[0];
	}

	@Override
	protected CertificateType getCertificateType() {
		return CertificateType.X509;
	}

	public ReloadCertificate getLocalCert() {
		return localCert;
	}

	public Set<ReloadCertificate> getStoredCerts() {
		return storedCerts;
	}

	public PrivateKey getPrivateKey() {
		return privateKey;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((localCert == null) ? 0 : localCert.hashCode());
		result = prime * result + ((getConfiguration().getOverlayName() == null) ? 0 : getConfiguration().getOverlayName().hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		LocalBaseConnector other = (LocalBaseConnector) obj;

		if (getLocalNodeId() == null) {
			if (other.getLocalNodeId() != null)
				return false;
		} else if (!getLocalNodeId().equals(other.getLocalNodeId()))
			return false;

		if (getConfiguration().getOverlayName() == null) {
			if (other.getConfiguration().getOverlayName() != null)
				return false;
		} else if (!getConfiguration().getOverlayName().equals(other.getConfiguration().getOverlayName()))
			return false;

		return true;
	}
}
