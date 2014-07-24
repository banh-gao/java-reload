package com.github.reload.net.stack;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import com.github.reload.Components;
import com.github.reload.Components.MessageHandler;
import com.github.reload.conf.Configuration;
import com.github.reload.net.MessageRouter;
import com.github.reload.net.NetworkTest;
import com.github.reload.net.TestConfiguration;
import com.github.reload.net.connections.Connection;
import com.github.reload.net.connections.ConnectionManager;
import com.github.reload.net.encoders.Message;
import com.github.reload.net.encoders.content.ContentType;
import com.github.reload.net.encoders.content.PingAnswer;
import com.github.reload.net.encoders.content.PingRequest;
import com.github.reload.net.encoders.header.DestinationList;
import com.github.reload.net.encoders.header.Header;
import com.github.reload.net.encoders.secBlock.GenericCertificate;
import com.github.reload.net.encoders.secBlock.SecurityBlock;
import com.github.reload.net.encoders.secBlock.Signature;
import com.github.reload.net.ice.IceCandidate.OverlayLinkType;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

public class RouterTest extends NetworkTest {

	static MessageRouter msgRouter;
	static Message answer;

	@BeforeClass
	public static void init() throws Exception {
		Configuration conf = new TestConfiguration();

		ConnectionManager connMgr = new ConnectionManager();
		msgRouter = new MessageRouter();

		Components.register(new TestListener());
		Components.register(new TestRouting());
		Components.register(new TestCrypto());
		Components.register(msgRouter);
		Components.register(conf);
		Components.register(connMgr);

		Components.initComponents();

		ListenableFuture<Connection> c = connMgr.connectTo(TEST_NODEID, new InetSocketAddress(InetAddress.getLocalHost(), TEST_PORT), OverlayLinkType.TLS_TCP_FH_NO_ICE);

		Futures.get(c, 50, TimeUnit.MILLISECONDS, Exception.class);
	}

	@AfterClass
	public static void deinit() {
		Components.deinitComponents();
	}

	@Test
	public void testSendMessage() throws Exception {
		Message m = new Message(new Header.Builder().setDestinationList(new DestinationList(TEST_NODEID)).build(), new PingRequest(), new SecurityBlock(new ArrayList<GenericCertificate>(), Signature.EMPTY_SIGNATURE));
		msgRouter.sendMessage(m);

		synchronized (msgRouter) {
			msgRouter.wait(50);
		}

		Assert.assertNotNull(answer);
		Assert.assertEquals(m.getHeader().getTransactionId(), answer.getHeader().getTransactionId());
	}

	@Test
	public void testRequestAnswer() throws Exception {
		Header header = new Header.Builder().setDestinationList(new DestinationList(TEST_NODEID)).build();
		Message req = new Message(header, new PingRequest(), new SecurityBlock(new ArrayList<GenericCertificate>(), Signature.EMPTY_SIGNATURE));
		ListenableFuture<Message> ansFut = msgRouter.sendRequestMessage(req);

		Message ans = new Message(header, new PingAnswer(2, BigInteger.ONE), new SecurityBlock(new ArrayList<GenericCertificate>(), Signature.EMPTY_SIGNATURE));
		msgRouter.sendMessage(ans);

		Message rcvAns = Futures.get(ansFut, 100, TimeUnit.MILLISECONDS, Exception.class);

		Assert.assertNotNull(answer);
		Assert.assertEquals(ans.getHeader().getTransactionId(), rcvAns.getHeader().getTransactionId());
	}

	static class TestListener {

		@MessageHandler(ContentType.PING_REQ)
		public void messageReceived(Message message) {
			answer = message;
			synchronized (msgRouter) {
				msgRouter.notify();
			}
		}
	}
}
