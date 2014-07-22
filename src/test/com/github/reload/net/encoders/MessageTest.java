package com.github.reload.net.encoders;

import io.netty.channel.ChannelFuture;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import com.github.reload.Components;
import com.github.reload.Configuration;
import com.github.reload.net.MessageRouter;
import com.github.reload.net.NetworkTest;
import com.github.reload.net.connections.Connection;
import com.github.reload.net.connections.ConnectionManager;
import com.github.reload.net.encoders.content.ContentType;
import com.github.reload.net.ice.IceCandidate.OverlayLinkType;
import com.github.reload.net.stack.MessageDispatcher;
import com.github.reload.net.stack.MessageDispatcher.MessageHandler;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

public class MessageTest extends NetworkTest {

	private static Connection conn;
	private static MessageRouter msgRouter;
	private static Message echo;

	protected Message sendMessage(Message message) throws Exception {

		ChannelFuture f = conn.write(message);

		f.await(50);

		if (f.cause() != null) {
			System.out.println(f.cause());
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
		Configuration conf = new Configuration();
		ConnectionManager connMgr = new ConnectionManager();
		MessageRouter msgRouter = new MessageRouter();
		MessageDispatcher msgDispatcher = new MessageDispatcher(Executors.newSingleThreadExecutor(), msgRouter);
		msgDispatcher.registerHandler(new TestListener());

		Components.register(new TestRouting());
		Components.register(new TestCrypto());
		Components.register(msgDispatcher);
		Components.register(msgRouter);
		Components.register(conf);
		Components.register(connMgr);

		Components.initComponents();

		ListenableFuture<Connection> c = connMgr.connectTo(TEST_NODEID, new InetSocketAddress(InetAddress.getLocalHost(), TEST_PORT), OverlayLinkType.TLS_TCP_FH_NO_ICE);

		conn = Futures.get(c, 50, TimeUnit.MILLISECONDS, Exception.class);
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

	@AfterClass
	public static void deinitPipeline() throws InterruptedException {
		Components.deinitComponents();
	}
}
