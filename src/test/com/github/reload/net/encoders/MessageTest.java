package com.github.reload.net.encoders;

import io.netty.channel.ChannelFuture;
import java.util.concurrent.TimeUnit;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import com.github.reload.Components;
import com.github.reload.Components.MessageHandler;
import com.github.reload.net.NetworkTest;
import com.github.reload.net.connections.Connection;
import com.github.reload.net.connections.ConnectionManager;
import com.github.reload.net.encoders.content.ContentType;
import com.github.reload.net.ice.HostCandidate.OverlayLinkType;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

public class MessageTest extends NetworkTest {

	private static Connection conn;

	private static Message echo;

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

	private static class TestListener {

		@MessageHandler(ContentType.UNKNOWN)
		public void requestReceived(Message message) {
			echo = message;

			synchronized (conn) {
				conn.notify();
			}
		}
	}

	@BeforeClass
	public static void initPipeline() throws Exception {

		Components.registerMessageHandler(new TestListener());

		Components.initComponents();

		ConnectionManager connMgr = (ConnectionManager) Components.get(ConnectionManager.COMPNAME);

		ListenableFuture<Connection> c = connMgr.connectTo(SERVER_ADDR, OverlayLinkType.TLS_TCP_FH_NO_ICE);

		conn = Futures.get(c, 500, TimeUnit.MILLISECONDS, Exception.class);
	}

	@AfterClass
	public static void deinitPipeline() throws InterruptedException {
		Components.deinitComponents();
	}
}
