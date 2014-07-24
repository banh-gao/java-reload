package com.github.reload.net.encoders;

import io.netty.channel.ChannelFuture;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import com.github.reload.Components;
import com.github.reload.Components.Component;
import com.github.reload.Components.MessageHandler;
import com.github.reload.ReloadConnector;
import com.github.reload.crypto.CryptoHelper;
import com.github.reload.net.NetworkTest;
import com.github.reload.net.TestConfiguration;
import com.github.reload.net.connections.Connection;
import com.github.reload.net.connections.ConnectionManager;
import com.github.reload.net.encoders.content.ContentType;
import com.github.reload.net.encoders.secBlock.GenericCertificate.CertificateType;
import com.github.reload.net.ice.IceCandidate.OverlayLinkType;
import com.github.reload.routing.TopologyPlugin;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

public class MessageTest extends NetworkTest {

	private static Connection conn;
	private static Message echo;

	private static InetSocketAddress SERVER_ADDR;

	protected Message sendMessage(Message message) throws Exception {

		ChannelFuture f = conn.write(message);

		f.await(50);

		if (f.cause() != null) {
			throw new Exception(f.cause());
		}

		synchronized (conn) {
			conn.wait(50);
		}

		Assert.assertNotNull(echo);

		return echo;

	}

	@BeforeClass
	public static void initPipeline() throws Exception {
		SERVER_ADDR = new InetSocketAddress(InetAddress.getLocalHost(), 6084);
		ConnectionManager connMgr = new ConnectionManager();

		TestConnector testConn = new TestConnector();
		testConn.setLocalAddress(SERVER_ADDR);

		Components.register(new TestListener());
		Components.register(new TestRouting());
		Components.register(new TestCrypto());
		Components.register(new TestConfiguration());
		Components.register(testConn);
		Components.register(connMgr);
		Components.initComponents();

		ListenableFuture<Connection> c = connMgr.connectTo(TEST_NODEID, ECHO_SERVER_ADDR, OverlayLinkType.TLS_TCP_FH_NO_ICE);

		conn = Futures.get(c, 100, TimeUnit.MILLISECONDS, Exception.class);
	}

	static class TestListener {

		@MessageHandler(ContentType.UNKNOWN)
		public void requestReceived(Message message) {
			echo = message;

			synchronized (conn) {
				conn.notify();
			}
		}
	}

	@Component(ReloadConnector.class)
	public static class TestConnector extends ReloadConnector {

		@Override
		protected byte[] getJoinData() {
			return new byte[0];
		}

		@Override
		protected TopologyPlugin getTopologyPlugin() {
			return null;
		}

		@Override
		protected CertificateType getCertificateType() {
			return CertificateType.X509;
		}

		@Override
		protected CryptoHelper getCryptoHelper() {
			return new TestCrypto();
		}

		@Override
		public boolean equals(Object obj) {
			return false;
		}

		@Override
		public int hashCode() {
			return 0;
		}

	}

	@AfterClass
	public static void deinitPipeline() throws InterruptedException {
		Components.deinitComponents();
	}
}
