package com.github.reload;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import org.junit.Test;
import com.github.reload.net.TestConfiguration;
import com.github.reload.net.encoders.header.NodeID;
import com.google.common.util.concurrent.ListenableFuture;

public class APITest {

	public static NodeID TEST_NODEID = NodeID.valueOf("f16a536ca4028b661fcb864a075f3871");

	@Test
	public void testAPI() throws Exception {
		BootstrapFactory.register(new TestFactory());
		Bootstrap b = BootstrapFactory.createBootstrap(new TestConfiguration());
		b.setOverlayInitiator(true);
		b.setLocalAddress(new InetSocketAddress(InetAddress.getLoopbackAddress(), 6084));
		b.setLocalNodeId(TEST_NODEID);

		ListenableFuture<Overlay> ovrFut = b.connect();
		Overlay overlay = ovrFut.get();

		synchronized (this) {
			wait();
		}

		// TODO Auto-generated constructor stub
	}
}
