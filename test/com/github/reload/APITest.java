package com.github.reload;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import com.github.reload.net.encoders.header.NodeID;
import com.github.reload.net.encoders.secBlock.SignatureAlgorithm;

public abstract class APITest {

	public static NodeID TEST_NODEID = NodeID.valueOf("f16a536ca4028b661fcb864a075f3871");

	public static Overlay overlay;

	@BeforeClass
	public static void init() throws Exception {
		BootstrapFactory.register(new TestFactory());

		Bootstrap b = BootstrapFactory.createBootstrap(new TestConfiguration());

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
