package com.github.reload.net.stack;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import com.github.reload.Components;
import com.github.reload.Configuration;
import com.github.reload.net.MessageRouter;
import com.github.reload.net.NetworkTest;
import com.github.reload.net.connections.Connection;
import com.github.reload.net.connections.ConnectionManager;
import com.github.reload.net.encoders.Message;
import com.github.reload.net.encoders.content.Content;
import com.github.reload.net.encoders.content.ContentType;
import com.github.reload.net.encoders.content.PingRequest;
import com.github.reload.net.encoders.header.Header;
import com.github.reload.net.encoders.secBlock.GenericCertificate;
import com.github.reload.net.encoders.secBlock.SecurityBlock;
import com.github.reload.net.encoders.secBlock.Signature;
import com.github.reload.net.ice.IceCandidate.OverlayLinkType;
import com.github.reload.net.stack.MessageDispatcher.MessageHandler;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

public class MessageDispatcherTest extends NetworkTest {

	private static Connection conn;
	private static Message answer;

	@BeforeClass
	public static void init() throws Exception {
		Configuration conf = new Configuration();
		conf.setOverlayAttribute(Configuration.NODE_ID_LENGTH, 5);

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

		msgDispatcher.registerHandler(new TestListener());
	}

	@Test
	public void testDispatch() throws Exception {

		Header h = new Header.Builder().build();
		Content content = new PingRequest(75);
		SecurityBlock s = new SecurityBlock(new ArrayList<GenericCertificate>(), Signature.EMPTY_SIGNATURE);

		Message message = new Message(h, content, s);

		conn.write(message);

		synchronized (this) {
			wait(1000);
		}

		Assert.assertNotNull(answer);
		Assert.assertEquals(message.getHeader().getTransactionId(), answer.getHeader().getTransactionId());
	}

	@AfterClass
	public static void deinit() {
		Components.deinitComponents();
	}

	static class TestListener {

		@MessageHandler(ContentType.UNKNOWN)
		public void messageReceived(Message message) {
			answer = message;
			synchronized (conn) {
				conn.notify();
			}
		}
	}
}