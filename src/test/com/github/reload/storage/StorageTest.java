package com.github.reload.storage;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import com.github.reload.Bootstrap;
import com.github.reload.BootstrapFactory;
import com.github.reload.Overlay;
import com.github.reload.TestFactory;
import com.github.reload.net.TestConfiguration;
import com.github.reload.net.encoders.header.NodeID;
import com.google.common.util.concurrent.ListenableFuture;

public class StorageTest {

	public static NodeID TEST_NODEID = NodeID.valueOf("f16a536ca4028b661fcb864a075f3871");
	private static Overlay overlay;
	private static StorageService storServ;

	@BeforeClass
	public static void init() throws Exception {
		BootstrapFactory.register(new TestFactory());
		Bootstrap b = BootstrapFactory.createBootstrap(new TestConfiguration());

		b.setLocalAddress(new InetSocketAddress(InetAddress.getLoopbackAddress(), 6084));
		b.setLocalNodeId(TEST_NODEID);
		b.setClientMode(true);

		ListenableFuture<Overlay> ovrFut = b.connect();
		overlay = ovrFut.get();
		storServ = overlay.getService(StorageService.SERVICE_ID);
	}

	@Test
	public void testStore() {
		storServ.storeData(destination, preparedData);
	}

	@AfterClass
	public static void deinit() {
		overlay.leave();
	}
}
