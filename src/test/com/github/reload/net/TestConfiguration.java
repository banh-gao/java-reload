package com.github.reload.net;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URL;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.github.reload.Components.Component;
import com.github.reload.conf.Configuration;
import com.github.reload.crypto.ReloadCertificate;
import com.github.reload.net.encoders.content.errors.UnknownKindException;
import com.github.reload.net.encoders.header.NodeID;
import com.github.reload.net.encoders.secBlock.HashAlgorithm;
import com.github.reload.net.encoders.secBlock.Signature;
import com.github.reload.net.ice.IceCandidate.OverlayLinkType;
import com.github.reload.storage.DataKind;

/**
 * Representation of a RELOAD configuration document
 */
@Component(Configuration.COMPNAME)
public class TestConfiguration implements Configuration {

	String instanceName;
	int sequence;
	Date expiration;
	int reliabilityTimer;
	String topologyPlugin;
	long maxMessageSize;
	short initialTTL;
	List<? extends Certificate> rootCerts;
	Map<Long, DataKind> requiredKinds;
	List<URL> enrollmentServers;
	List<NodeID> kindSigners;
	List<NodeID> configurationSigners;
	List<NodeID> badNodes;
	boolean noICE;
	String sharedSecret;
	List<String> linkProtocols;
	boolean clientPermitted;
	short turnDensity;
	short nodeIdLength;
	List<String> mandatoryExtensions;
	boolean selfSignedPermitted;
	HashAlgorithm selfSignedDigestType;
	Set<InetSocketAddress> bootstrapNodes;
	byte[] rawXml;
	Signature signature;
	private ReloadCertificate rootCert;

	public TestConfiguration() throws Exception {
		Certificate CA_CERT = (X509Certificate) loadLocalCert("CAcert.der");
		rootCerts = Collections.singletonList(CA_CERT);
		instanceName = "testOverlay.com";
		maxMessageSize = 5000;
		initialTTL = 6;
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

	/**
	 * @return The overlay name
	 */
	public String getOverlayName() {
		return instanceName;
	}

	public int getConfigurationSequence() {
		return sequence;
	}

	public Date getExpiration() {
		return expiration;
	}

	public String getTopologyPlugin() {
		return topologyPlugin;
	}

	/**
	 * @return the length in bytes of the nodeids used in this overlay
	 */
	public int getNodeIdLength() {
		return nodeIdLength;
	}

	/**
	 * @return the maximum size of a message allowed in the overlay in bytes
	 */
	public int getMaxMessageSize() {
		return (int) maxMessageSize;
	}

	/**
	 * @return the maximum time in milliseconds to wait until a request should
	 *         be considerated unreplied
	 */
	public int getReliabilityTimer() {
		return reliabilityTimer;
	}

	/**
	 * @return the initial value of the time to live field of the forwarding
	 *         header
	 */
	public short getInitialTTL() {
		return initialTTL;
	}

	public DataKind getDataKind(long kindId) throws UnknownKindException {
		DataKind k = requiredKinds.get(kindId);
		if (k == null)
			throw new UnknownKindException("Unregistered data kind " + kindId);
		return k;
	}

	public Set<Long> getDataKindIds() {
		return Collections.unmodifiableSet(requiredKinds.keySet());
	}

	public List<NodeID> getKindSigners() {
		return Collections.unmodifiableList(kindSigners);
	}

	public Set<InetSocketAddress> getBootstrapNodes() {
		return Collections.unmodifiableSet(bootstrapNodes);
	}

	public List<URL> getEnrollmentServers() {
		return Collections.unmodifiableList(enrollmentServers);
	}

	public List<Certificate> getRootCerts() {
		return Collections.unmodifiableList(rootCerts);
	}

	public boolean admitSelfSigned() {
		return selfSignedPermitted;
	}

	public List<NodeID> getBadNodes() {
		return Collections.unmodifiableList(badNodes);
	}

	public List<String> getMandatoryExtensions() {
		return Collections.unmodifiableList(mandatoryExtensions);
	}

	public List<String> getLinkProtocols() {
		return Collections.unmodifiableList(linkProtocols);
	}

	public short getTurnDensity() {
		return turnDensity;
	}

	public HashAlgorithm getSelfSignedDigestType() {
		return selfSignedDigestType;
	}

	public String getSharedSecret() {
		return sharedSecret;
	}

	public boolean isClientPermitted() {
		return clientPermitted;
	}

	public boolean isNoICE() {
		return noICE;
	}

	public Set<OverlayLinkType> getOverlayLinkTypes() {
		Set<OverlayLinkType> out = new HashSet<OverlayLinkType>();

		if (isNoICE() == false) {
			out.add(OverlayLinkType.DTLS_UDP_SR);
			return out;
		}

		for (String proto : getLinkProtocols()) {
			if ("DTLS".equalsIgnoreCase(proto)) {
				out.add(OverlayLinkType.DTLS_UDP_SR_NO_ICE);
			} else if ("TLS".equalsIgnoreCase(proto)) {
				out.add(OverlayLinkType.TLS_TCP_FH_NO_ICE);
			}
		}

		return Collections.unmodifiableSet(out);
	}

	public List<NodeID> getConfSigners() {
		return Collections.unmodifiableList(configurationSigners);
	}

	public boolean isSelfSignedPermitted() {
		return selfSignedPermitted;
	}

	public byte[] getRawXML() {
		return rawXml;
	}

	public Signature getSignature() {
		return signature;
	}

	public ReloadCertificate getRootCertificate() {
		return rootCert;
	}

	@Override
	public String toString() {
		return "XMLConfiguration [instanceName=" + instanceName + ", reliabilityTimer=" + reliabilityTimer + ", topologyPlugin=" + topologyPlugin + ", maxMessageSize=" + maxMessageSize + ", initialTTL=" + initialTTL + ", rootCerts=" + rootCerts + ", requiredKinds=" + requiredKinds + ", enrollmentServers=" + enrollmentServers + ", kindSigners=" + kindSigners + ", configurationSigners=" + configurationSigners + ", badNodes=" + badNodes + ", noICE=" + noICE + ", sharedSecret=" + sharedSecret + ", linkProtocols=" + linkProtocols + ", clientPermitted=" + clientPermitted + ", turnDensity=" + turnDensity + ", nodeIdLength=" + nodeIdLength + ", mandatoryExtensions=" + mandatoryExtensions + ", selfSignedPermitted=" + selfSignedPermitted + ", selfSignedDigestType=" + selfSignedDigestType + ", bootstrapNodes=" + bootstrapNodes + "]";
	}
}
