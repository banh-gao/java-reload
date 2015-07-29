package com.github.reload;

import io.netty.util.AttributeKey;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import com.github.reload.conf.Configuration;
import com.github.reload.net.ice.HostCandidate.OverlayLinkType;
import com.github.reload.services.storage.DataKind;
import com.github.reload.services.storage.DataModel;
import com.github.reload.services.storage.policies.UserMatch;

/**
 * Representation of a RELOAD configuration document
 */
public class TestConfiguration implements Configuration {

	public static InetSocketAddress BOOTSTRAP_ADDR = new InetSocketAddress(InetAddress.getLoopbackAddress(), 6084);
	public static DataKind TEST_KIND_SINGLE = new DataKind.Builder(2020).dataModel(DataModel.SINGLE).accessPolicy(UserMatch.class).build();
	public static DataKind TEST_KIND_ARRAY = new DataKind.Builder(2050).dataModel(DataModel.ARRAY).accessPolicy(UserMatch.class).build();
	public static DataKind TEST_KIND_DICT = new DataKind.Builder(2070).dataModel(DataModel.DICTIONARY).accessPolicy(UserMatch.class).build();

	private final Map<AttributeKey<?>, Object> conf = new HashMap<AttributeKey<?>, Object>();

	public TestConfiguration() throws Exception {
		set(ROOT_CERTS, Collections.singletonList((X509Certificate) loadLocalCert("CAcert.der")));
		set(OVERLAY_NAME, "testOverlay.com");
		set(MAX_MESSAGE_SIZE, 5000);
		set(NODE_ID_LENGTH, 16);
		set(INITIAL_TTL, (short) 6);
		set(LINK_PROTOCOLS, Collections.singleton("TLS"));
		set(BOOT_NODES, Collections.singleton(BOOTSTRAP_ADDR));
		set(NO_ICE, true);
		set(LINK_TYPES, getOverlayLinkTypes());
		set(TOPOLOGY, "TEST");

		Set<DataKind> requiredKinds = new HashSet<DataKind>();
		requiredKinds.add(TEST_KIND_SINGLE);
		DataKind.registerDataKind(TEST_KIND_SINGLE);
		requiredKinds.add(TEST_KIND_ARRAY);
		DataKind.registerDataKind(TEST_KIND_ARRAY);
		requiredKinds.add(TEST_KIND_DICT);
		DataKind.registerDataKind(TEST_KIND_DICT);
		set(DATA_KINDS, requiredKinds);
	}

	public void setBootstrap(InetSocketAddress bootAddr) {
		set(BOOT_NODES, Collections.singleton(bootAddr));
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

	private Set<OverlayLinkType> getOverlayLinkTypes() {
		Set<OverlayLinkType> out = new HashSet<OverlayLinkType>();

		if (get(NO_ICE) == false) {
			out.add(OverlayLinkType.DTLS_UDP_SR);
			return out;
		}

		for (String proto : get(LINK_PROTOCOLS)) {
			if ("DTLS".equalsIgnoreCase(proto)) {
				out.add(OverlayLinkType.DTLS_UDP_SR_NO_ICE);
			} else if ("TLS".equalsIgnoreCase(proto)) {
				out.add(OverlayLinkType.TLS_TCP_FH_NO_ICE);
			}
		}

		return Collections.unmodifiableSet(out);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T get(AttributeKey<T> name) {
		return (T) conf.get(name);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T set(AttributeKey<T> name, T value) {
		return (T) conf.put(name, value);
	}
}
