package com.github.reload;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import com.github.reload.TestFactory.TestBootstrap;
import com.github.reload.conf.Configuration;
import com.github.reload.net.codecs.header.NodeID;
import com.github.reload.net.codecs.secBlock.SignatureAlgorithm;

public abstract class APITest {

	public static NodeID TEST_NODEID = NodeID.valueOf("f16a536ca4028b661fcb864a075f3871");

	public static final Configuration CONF = new TestConfiguration();

	public static Overlay overlay;

	static {
		BootstrapFactory.register(new TestFactory());
	}

	@BeforeClass
	public static void init() throws Exception {
		Bootstrap b = BootstrapFactory.newBootstrap(CONF);

		b.setLocalAddress(TestConfiguration.BOOTSTRAP_ADDR);

		b.setLocalNodeId(TEST_NODEID);
		b.setLocalCert(TestBootstrap.loadCert("certs/peer0_cert.der"));
		b.setLocalKey(TestBootstrap.loadPrivateKey("privKeys/peer0_key.der", SignatureAlgorithm.RSA));

		b.setOverlayInitiator(true);

		overlay = b.connect().get();
	}

	@AfterClass
	public static void deinit() throws InterruptedException {
		overlay.disconnect();
	}
}
