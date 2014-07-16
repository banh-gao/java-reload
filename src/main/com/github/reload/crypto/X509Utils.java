package com.github.reload.crypto;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.cert.X509Extension;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.bouncycastle.asn1.DERBitString;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.jce.PKCS10CertificationRequest;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import sun.security.x509.GeneralName;
import sun.security.x509.GeneralNames;
import sun.security.x509.RDN;
import sun.security.x509.X500Name;
import com.github.reload.ReloadUri;
import com.github.reload.net.encoders.header.DestinationList;
import com.github.reload.net.encoders.header.NodeID;
import com.github.reload.net.encoders.secBlock.HashAlgorithm;
import com.github.reload.net.encoders.secBlock.SecurityBlock;
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
	 */
	public static PKCS10CertificationRequest getCSR(PublicKey subjectPubKey, PrivateKey subjectPrivateKey, String commonName) throws IOException, OperatorCreationException {
		SubjectPublicKeyInfo keyInfo = SubjectPublicKeyInfo.getInstance(subjectPubKey.getEncoded());

		PKCS10CertificationRequestBuilder pk10b = new PKCS10CertificationRequestBuilder(new X500Name("CN=" + commonName), keyInfo);

		AlgorithmIdentifier sigAlgId = new DefaultSignatureAlgorithmIdentifierFinder().find(HASH_ALG.toString() + "with" + SIGN_ALG.toString());
		AlgorithmIdentifier digAlgId = new DefaultDigestAlgorithmIdentifierFinder().find(sigAlgId);

		AsymmetricKeyParameter subjectPriv = PrivateKeyFactory.createKey(subjectPrivateKey.getEncoded());
		ContentSigner signer = new BcRSAContentSignerBuilder(sigAlgId, digAlgId).build(subjectPriv);

		return pk10b.build(signer);
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
			AlgorithmIdentifier sigAlgId = new DefaultSignatureAlgorithmIdentifierFinder().find(HASH_ALG.toString() + "with" + SIGN_ALG.toString());
			AlgorithmIdentifier digAlgId = new DefaultDigestAlgorithmIdentifierFinder().find(sigAlgId);

			AsymmetricKeyParameter caPrivateParam = PrivateKeyFactory.createKey(signKey.getEncoded());
			SubjectPublicKeyInfo keyInfo = csr.getSubjectPublicKeyInfo();

			Calendar c = Calendar.getInstance();
			c.add(Calendar.YEAR, 1);

			X509v3CertificateBuilder certBuilder = new X509v3CertificateBuilder(csr.getSubject(), BigInteger.ONE, new Date(), c.getTime(), csr.getSubject(), keyInfo);

			List<GeneralName> genNames = generateGeneralNodeIds(csr.getSubjectPublicKeyInfo(), neededIds, nodeIdLength, overlayName);
			genNames.add(getGeneralFromCommonName(csr.getSubject()));
			GeneralNames encGenNames = new GeneralNames(genNames.toArray(new GeneralName[0]));
			certBuilder.addExtension(X509Extension.subjectAlternativeName, true, new X509Extension(true, new DEROctetString(encGenNames)).getParsedValue());

			ContentSigner sigGen = new BcRSAContentSignerBuilder(sigAlgId, digAlgId).build(caPrivateParam);
			X509CertificateHolder holder = certBuilder.build(sigGen);

			CertificateFactory cf = CertificateFactory.getInstance("X.509");

			// Read Certificate
			ByteBuffer tmp = ByteBuffer.allocate(holder.getEncoded().length);
			tmp.put(holder.getEncoded());
			X509Certificate cert = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(tmp.array()));
			return X509CertificateParser.parse(cert);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (OperatorCreationException e) {
			throw new RuntimeException(e);
		}
	}

	private static List<GeneralName> generateGeneralNodeIds(SubjectPublicKeyInfo pkInfo, int neededIds, int nodeIdLength, String overlayName) {
		byte[] encodedPubKey = extractPublicKey(pkInfo).getEncoded();
		List<GeneralName> idNames = new ArrayList<GeneralName>();

		for (int i = 1; i <= neededIds; i++) {
			NodeID node = getKeyBasedNodeId(i, encodedPubKey, nodeIdLength);
			ReloadUri uri = ReloadUri.create(DestinationList.create(node), overlayName);
			GeneralName subjectAltName = new GeneralName(GeneralName.uniformResourceIdentifier, uri.toASCIIString());
			idNames.add(subjectAltName);
		}
		return idNames;
	}

	private static PublicKey extractPublicKey(SubjectPublicKeyInfo pkInfo) {
		X509EncodedKeySpec xspec = new X509EncodedKeySpec(new DERBitString(pkInfo).getBytes());
		AlgorithmIdentifier keyAlg = pkInfo.getAlgorithm();

		try {
			return KeyFactory.getInstance(keyAlg.getAlgorithm().getId(), "BC").generatePublic(xspec);
		} catch (InvalidKeySpecException e) {
			throw new IllegalArgumentException("error decoding public key");
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalArgumentException(e);
		} catch (NoSuchProviderException e) {
			throw new IllegalArgumentException(e);
		}
	}

	private static GeneralName getGeneralFromCommonName(X500Name x500name) {
		RDN cn = x500name.getRDNs(BCStyle.CN)[0];
		String username = IETFUtils.valueToString(cn.getFirst().getValue());
		return new GeneralName(GeneralName.rfc822Name, username);
	}

	static NodeID getKeyBasedNodeId(int nodeIndex, byte[] DERPublicKey, int nodeIdLength) {
		MessageDigest digestor;
		try {
			digestor = MessageDigest.getInstance(HASH_ALG.toString());
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
		UnsignedByteBuffer buf = UnsignedByteBuffer.allocate(32);
		buf.putUnsigned32(nodeIndex);
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
			SignerIdentity signerId = SignerIdentity.singleIdIdentity(HASH_ALG, signerCert.getOriginalCertificate());
			GenericSignature sign;
			sign = new GenericSignature(signerId, HASH_ALG, SignatureAlgorithm.RSA);
			sign.initSign(signKey);
			sign.update(data);

			Set<Certificate> certs = new HashSet<Certificate>();
			certs.add(signerCert.getOriginalCertificate());
			return new SecurityBlock(certs, sign);
		} catch (Exception e) {
			throw new SignatureException(e);
		}
	}
}
