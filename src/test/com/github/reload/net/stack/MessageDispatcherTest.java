package com.github.reload.net.stack;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import org.junit.Assert;
import org.junit.Test;
import com.github.reload.Configuration;
import com.github.reload.net.NetworkTest;
import com.github.reload.net.encoders.Message;
import com.github.reload.net.encoders.content.Content;
import com.github.reload.net.encoders.content.PingRequest;
import com.github.reload.net.encoders.header.Header;
import com.github.reload.net.encoders.secBlock.GenericCertificate;
import com.github.reload.net.encoders.secBlock.SecurityBlock;
import com.github.reload.net.encoders.secBlock.Signature;
import com.github.reload.net.ice.IceCandidate.OverlayLinkType;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

public class MessageDispatcherTest extends NetworkTest {

	private Message answer;

	@Test
	public void testDispatch() throws Exception {
		Configuration conf = new Configuration();
		conf.setOverlayAttribute(Configuration.NODE_ID_LENGTH, 5);
		EventBus messageBus = new EventBus();
		ReloadStackBuilder b = new ReloadStackBuilder(conf, messageBus);
		b.setLinkType(OverlayLinkType.TLS_TCP_FH_NO_ICE);

		ReloadStack stack = b.buildStack();
		stack.connect(new InetSocketAddress(InetAddress.getLocalHost(), TEST_PORT)).sync();

		messageBus.register(new TestListener());

		Header h = new Header.Builder().build();
		Content content = new PingRequest(75);
		SecurityBlock s = new SecurityBlock(new ArrayList<GenericCertificate>(), Signature.EMPTY_SIGNATURE);

		Message message = new Message(h, content, s);

		stack.write(message);
		stack.flush();

		synchronized (this) {
			wait(1000);
		}

		Assert.assertNotNull(answer);
		Assert.assertEquals(message.getHeader().getTransactionId(), answer.getHeader().getTransactionId());

		stack.shutdown();
	}

	class TestListener {

		@Subscribe
		public void messageReceived(Message message) {
			answer = message;
			synchronized (MessageDispatcherTest.this) {
				MessageDispatcherTest.this.notify();
			}
		}
	}
}