package com.github.reload.crypto;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1Set;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.X509Extensions;
import org.bouncycastle.asn1.x509.X509Name;
import org.bouncycastle.jce.PKCS10CertificationRequest;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.x509.X509V3CertificateGenerator;
import com.github.reload.ReloadUri;
import com.github.reload.net.encoders.header.DestinationList;
import com.github.reload.net.encoders.header.NodeID;
import com.github.reload.net.encoders.secBlock.GenericCertificate;
import com.github.reload.net.encoders.secBlock.GenericCertificate.CertificateType;
import com.github.reload.net.encoders.secBlock.HashAlgorithm;
import com.github.reload.net.encoders.secBlock.SecurityBlock;
import com.github.reload.net.encoders.secBlock.Signature;
import com.github.reload.net.encoders.secBlock.SignatureAlgorithm;
import com.github.reload.net.encoders.secBlock.SignerIdentity;

/**
 * Utility class for PKI infrastructure functionalities
 * 
 */
public class X509Utils {

	public static final HashAlgorithm HASH_ALG = CryptoHelper.OVERLAY_HASHALG;
	public static final SignatureAlgorithm SIGN_ALG = SignatureAlgorithm.RSA;

	static {
		Security.addProvider(new BouncyCastleProvider());
	}

	/**
	 * Utility method to generate PKCS10 certificate signing requests from the
	 * specified arguments
	 * 
	 * @throws SignatureException
	 * @throws NoSuchProviderException
	 * @throws NoSuchAlgorithmException
	 * @throws InvalidKeyException
	 */
	public static PKCS10CertificationRequest getCSR(PublicKey subjectPubKey, PrivateKey subjectPrivateKey, String commonName) throws IOException, InvalidKeyException, NoSuchAlgorithmException, NoSuchProviderException, SignatureException {
		ASN1Set attributes = null;

		return new PKCS10CertificationRequest(HASH_ALG.toString() + "with" + SIGN_ALG.toString(), new X509Name("CN=" + commonName), subjectPubKey, attributes, subjectPrivateKey);
	}

	/**
	 * Generate a self-signed X509 certificate to be used in the overlay
	 * 
	 * @param csr
	 * @param signKey
	 * @param nodeidLength
	 * @param signAlg
	 * @return
	 * @throws CertificateException
	 */
	public static ReloadCertificate generateSelfSignedCert(PKCS10CertificationRequest csr, PrivateKey signKey, int neededIds, String overlayName, int nodeIdLength) throws CertificateException {
		try {

			Calendar c = Calendar.getInstance();
			c.add(Calendar.YEAR, 1);

			X509V3CertificateGenerator certBuilder = new X509V3CertificateGenerator();
			certBuilder.setSubjectDN(csr.getCertificationRequestInfo().getSubject());
			certBuilder.setSerialNumber(BigInteger.ONE);
			certBuilder.setNotBefore(new Date());
			certBuilder.setNotAfter(c.getTime());
			certBuilder.setPublicKey(csr.getPublicKey());

			List<GeneralName> genNames = generateGeneralNodeIds(csr.getPublicKey(), neededIds, nodeIdLength, overlayName);

			genNames.add(getGeneralFromCommonName(csr.getCertificationRequestInfo().getSubject()));

			GeneralNames encGenNames = new GeneralNames(ASN1Sequence.getInstance(genNames));
			certBuilder.addExtension(X509Extensions.SubjectAlternativeName, true, encGenNames);

			String sigAlgId = HASH_ALG.toString() + "with" + SIGN_ALG.toString();

			X509Certificate cert = certBuilder.generate(signKey, sigAlgId);

			return X509CertificateParser.parse(cert);
		} catch (InvalidKeyException | IllegalStateException
				| NoSuchAlgorithmException | SignatureException
				| IllegalArgumentException | NoSuchProviderException e) {
			throw new RuntimeException(e);
		}
	}

	private static List<GeneralName> generateGeneralNodeIds(PublicKey pkInfo, int neededIds, int nodeIdLength, String overlayName) {
		byte[] encodedPubKey = pkInfo.getEncoded();
		List<GeneralName> idNames = new ArrayList<GeneralName>();

		for (int i = 1; i <= neededIds; i++) {
			NodeID node = getKeyBasedNodeId(i, encodedPubKey, nodeIdLength);
			ReloadUri uri = ReloadUri.create(new DestinationList(node), overlayName);
			GeneralName subjectAltName = new GeneralName(GeneralName.uniformResourceIdentifier, uri.toASCIIString());
			idNames.add(subjectAltName);
		}
		return idNames;
	}

	private static GeneralName getGeneralFromCommonName(X509Name x500name) {
		String username = (String) x500name.getValues(X509Name.CN).firstElement();
		return new GeneralName(GeneralName.rfc822Name, username);
	}

	static NodeID getKeyBasedNodeId(int nodeIndex, byte[] DERPublicKey, int nodeIdLength) {
		MessageDigest digestor;
		try {
			digestor = MessageDigest.getInstance(HASH_ALG.toString());
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
		ByteBuffer buf = ByteBuffer.allocate(32);
		buf.putInt(nodeIndex);
		digestor.update(buf.array());
		final byte[] hash = digestor.digest(DERPublicKey);
		return NodeID.valueOf(Arrays.copyOfRange(hash, 0, nodeIdLength));
	}

	/**
	 * Load an X509 certificate from the specified file and parses as a Reload
	 * Certificate, both PEM and DER x.509 certificates are accepted
	 * 
	 * @param certFile
	 * @return
	 * @throws FileNotFoundException
	 * @throws CertificateException
	 */
	public static ReloadCertificate readCert(File certFile) throws FileNotFoundException, CertificateException {
		if (certFile == null)
			throw new NullPointerException();

		if (!certFile.exists())
			throw new FileNotFoundException("Overlay certificate file not found at " + certFile.getAbsolutePath());

		CertificateFactory x509CertFactory;
		try {
			x509CertFactory = CertificateFactory.getInstance("x.509");
			InputStream certStream = new FileInputStream(certFile);
			X509Certificate cert = (X509Certificate) x509CertFactory.generateCertificate(certStream);
			certStream.close();
			return X509CertificateParser.parse(cert);
		} catch (CertificateException e) {
			throw new CertificateException(e);
		} catch (IOException e) {
			throw new CertificateException(e);
		}
	}

	public static SecurityBlock signData(ReloadCertificate signerCert, PrivateKey signKey, byte[] data) throws SignatureException {
		try {

			java.security.Signature sign = java.security.Signature.getInstance(SIGN_ALG.toString());
			sign.initSign(signKey);
			sign.update(data);

			byte[] digest = sign.sign();

			SignerIdentity signerId = SignerIdentity.singleIdIdentity(HASH_ALG, signerCert.getOriginalCertificate());

			List<GenericCertificate> certs = new ArrayList<GenericCertificate>();
			certs.add(new GenericCertificate(CertificateType.X509, signerCert.getOriginalCertificate()));
			return new SecurityBlock(certs, new Signature(signerId, HASH_ALG, SIGN_ALG, digest));
		} catch (Exception e) {
			throw new SignatureException(e);
		}
	}
}
