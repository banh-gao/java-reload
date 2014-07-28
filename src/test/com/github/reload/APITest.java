package com.github.reload;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import com.github.reload.appAttach.AppAttachService;
import com.github.reload.net.TestConfiguration;
import com.github.reload.net.encoders.header.NodeID;
import com.google.common.util.concurrent.ListenableFuture;

public class APITest {

	public static NodeID TEST_NODEID = NodeID.valueOf("f16a536ca4028b661fcb864a075f3871");

	public static Overlay overlay;

	@BeforeClass
	public static void init() throws Exception {
		Components.register(new AppAttachService());
		BootstrapFactory.register(new TestFactory());
		Bootstrap b = BootstrapFactory.createBootstrap(new TestConfiguration());

		b.setLocalAddress(new InetSocketAddress(InetAddress.getLoopbackAddress(), 6084));
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
