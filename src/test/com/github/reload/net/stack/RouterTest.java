package com.github.reload.net.stack;

import java.math.BigInteger;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import com.github.reload.Components;
import com.github.reload.Components.Component;
import com.github.reload.Components.MessageHandler;
import com.github.reload.net.MessageRouter;
import com.github.reload.net.NetworkTest;
import com.github.reload.net.connections.Connection;
import com.github.reload.net.connections.ConnectionManager;
import com.github.reload.net.encoders.Message;
import com.github.reload.net.encoders.MessageBuilder;
import com.github.reload.net.encoders.content.ContentType;
import com.github.reload.net.encoders.content.PingAnswer;
import com.github.reload.net.encoders.content.PingRequest;
import com.github.reload.net.encoders.header.DestinationList;
import com.github.reload.net.encoders.header.NodeID;
import com.github.reload.net.encoders.header.RoutableID;
import com.github.reload.net.ice.IceCandidate.OverlayLinkType;
import com.github.reload.routing.RoutingTable;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

public class RouterTest extends NetworkTest {

	static MessageRouter msgRouter;
	static Message answer;

	@BeforeClass
	public static void init() throws Exception {

		msgRouter = new MessageRouter();
		Components.register(msgRouter);

		Components.register(new TestRouting());

		Components.registerMessageHandler(new TestListener());

		Components.initComponents();

		ConnectionManager connMgr = (ConnectionManager) Components.get(ConnectionManager.COMPNAME);

		ListenableFuture<Connection> c = connMgr.connectTo(SERVER_ADDR, OverlayLinkType.TLS_TCP_FH_NO_ICE);

		Futures.get(c, 500, TimeUnit.MILLISECONDS, Exception.class);
	}

	@AfterClass
	public static void deinit() {
		Components.deinitComponents();
	}

	@Test
	public void testSendMessage() throws Exception {

		MessageBuilder b = (MessageBuilder) Components.get(MessageBuilder.COMPNAME);

		Message m = b.newMessage(new PingRequest(), new DestinationList(TEST_NODEID));

		msgRouter.sendMessage(m);

		synchronized (msgRouter) {
			msgRouter.wait(50);
		}

		Assert.assertNotNull(answer);
		Assert.assertEquals(m.getHeader().getTransactionId(), answer.getHeader().getTransactionId());
	}

	@Test
	public void testRequestAnswer() throws Exception {
		MessageBuilder b = (MessageBuilder) Components.get(MessageBuilder.COMPNAME);

		Message req = b.newMessage(new PingRequest(), new DestinationList(TEST_NODEID));

		ListenableFuture<Message> ansFut = msgRouter.sendRequestMessage(req);

		Message ans = b.newResponseMessage(req.getHeader(), new PingAnswer(2, BigInteger.ONE));
		msgRouter.sendMessage(ans);

		Message rcvAns = Futures.get(ansFut, 100, TimeUnit.MILLISECONDS, Exception.class);

		Assert.assertNotNull(answer);
		Assert.assertEquals(ans.getHeader().getTransactionId(), rcvAns.getHeader().getTransactionId());
	}

	private static class TestListener {

		@MessageHandler(ContentType.PING_REQ)
		public void messageReceived(Message message) {
			answer = message;
			synchronized (msgRouter) {
				msgRouter.notify();
			}
		}
	}

	@Component(RoutingTable.COMPNAME)
	public static class TestRouting implements RoutingTable {

		@Override
		public Set<NodeID> getNextHops(RoutableID destination) {
			return Collections.singleton(TEST_NODEID);
		}

		@Override
		public Set<NodeID> getNextHops(RoutableID destination, Collection<? extends NodeID> excludedIds) {
			return Collections.singleton(TEST_NODEID);
		}

	}
}
