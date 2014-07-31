package com.github.reload;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import com.github.reload.net.encoders.header.NodeID;
import com.github.reload.net.encoders.secBlock.SignatureAlgorithm;
import com.google.common.util.concurrent.ListenableFuture;

public abstract class APITest {

	public static NodeID TEST_NODEID = NodeID.valueOf("f16a536ca4028b661fcb864a075f3871");

	public static Overlay overlay;

	@BeforeClass
	public static void init() throws Exception {
		BootstrapFactory.register(new TestFactory());

		TestBootstrap b = (TestBootstrap) BootstrapFactory.createBootstrap(new TestConfiguration());

		b.setLocalAddress(TestConfiguration.BOOTSTRAP_ADDR);
		b.setLocalNodeId(TEST_NODEID);
		b.setClientMode(true);
		b.setLocalCert(TestBootstrap.loadCert("certs/peer0_cert.der"));
		b.setLocalKey(TestBootstrap.loadPrivateKey("privKeys/peer0_key.der", SignatureAlgorithm.RSA));

		ListenableFuture<Overlay> ovrFut = b.connect();
		overlay = ovrFut.get();
	}

	@AfterClass
	public static void deinit() throws InterruptedException {
		overlay.leave();
	}
}
