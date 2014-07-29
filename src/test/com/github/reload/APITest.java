package com.github.reload;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import com.github.reload.components.ComponentsRepository;
import com.github.reload.net.encoders.header.NodeID;
import com.google.common.util.concurrent.ListenableFuture;

public abstract class APITest {

	public static NodeID TEST_NODEID = NodeID.valueOf("f16a536ca4028b661fcb864a075f3871");

	public static Overlay overlay;

	@BeforeClass
	public static void init() throws Exception {
		BootstrapFactory.register(new TestFactory());

		ComponentsRepository.register(TestConfiguration.class);

		Bootstrap b = BootstrapFactory.createBootstrap(new TestConfiguration());

		b.setLocalAddress(TestBootstrap.SERVER_ADDR);
		b.setLocalNodeId(TEST_NODEID);
		b.setClientMode(true);

		ListenableFuture<Overlay> ovrFut = b.connect();
		overlay = ovrFut.get();
	}

	@AfterClass
	public static void deinit() throws InterruptedException {
		overlay.leave();
	}
}
