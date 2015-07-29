package com.github.reload;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import org.junit.AfterClass;
import org.junit.Test;
import com.github.reload.crypto.ReloadCertificate;
import com.github.reload.net.MessageRouter;
import com.github.reload.net.encoders.Message;
import com.github.reload.net.encoders.MessageBuilder;
import com.github.reload.net.encoders.content.PingRequest;
import com.github.reload.net.encoders.header.DestinationList;
import com.github.reload.net.encoders.header.NodeID;
import com.github.reload.net.encoders.secBlock.SignatureAlgorithm;
import com.google.common.util.concurrent.ListenableFuture;
import dagger.ObjectGraph;

public class MultiNodeTest {

	public static final int NUM_NODES = 2;

	public static Overlay[] overlays;

	@Test
	public void init() throws Exception {
		BootstrapFactory.register(new TestFactory());

		overlays = new Overlay[NUM_NODES];

		overlays[0] = startNode(TestConfiguration.BOOTSTRAP_ADDR, 0);

		for (int i = 1; i < NUM_NODES; i++) {
			overlays[i] = startNode(new InetSocketAddress(InetAddress.getLoopbackAddress(), 2000 + i), i);
		}

		ObjectGraph ctx = overlays[1].graph;

		Message req = ctx.get(MessageBuilder.class).newMessage(new PingRequest(), new DestinationList(NodeID.valueOf("ceeadf392596529d0f6aaabe39fbb116")));

		ctx.get(MessageRouter.class).sendRequestMessage(req);
	}

	private Overlay startNode(InetSocketAddress addr, int i) throws Exception {
		ReloadCertificate cert = TestBootstrap.loadCert("certs/peer" + i + "_cert.der");

		TestConfiguration c = new TestConfiguration();
		// c.setBootstrap(addr);

		TestBootstrap b = (TestBootstrap) BootstrapFactory.newBootstrap(c);

		if (addr.equals(TestConfiguration.BOOTSTRAP_ADDR)) {
			b.setOverlayInitiator(true);
		}

		b.setLocalAddress(addr);
		b.setLocalNodeId(cert.getNodeId());
		b.setLocalCert(cert);
		b.setLocalKey(TestBootstrap.loadPrivateKey("privKeys/peer" + i + "_key.der", SignatureAlgorithm.RSA));

		ListenableFuture<Overlay> ovrFut = b.connect();
		return ovrFut.get();
	}

	@AfterClass
	public static void deinit() throws InterruptedException {
		for (Overlay o : overlays) {
			o.disconnect();
		}
	}
}
