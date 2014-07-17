package com.github.reload.net.encoders;

import io.netty.channel.ChannelFuture;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import com.github.reload.Configuration;
import com.github.reload.net.MessageBus;
import com.github.reload.net.NetworkTest;
import com.github.reload.net.ice.IceCandidate.OverlayLinkType;
import com.github.reload.net.stack.ReloadStack;
import com.github.reload.net.stack.ReloadStackBuilder;
import com.google.common.eventbus.Subscribe;

public class MessageTest extends NetworkTest {

	private static ReloadStack stack;
	private static Message echo;

	protected Message sendMessage(Message message) throws Exception {

		ChannelFuture f = stack.write(message);
		stack.flush();

		f.await(50);

		if (f.cause() != null) {
			System.out.println(f.cause());
			throw new Exception(f.cause());
		}

		synchronized (stack) {
			stack.wait(50);
		}

		Assert.assertNotNull(echo);

		return echo;
	}

	@BeforeClass
	public static void initPipeline() throws Exception {
		Configuration conf = new Configuration();
		MessageBus messageBus = new MessageBus();
		ReloadStackBuilder b = new ReloadStackBuilder(conf, messageBus);
		b.setLinkType(OverlayLinkType.TLS_TCP_FH_NO_ICE);

		stack = b.buildStack();
		stack.connect(new InetSocketAddress(InetAddress.getLocalHost(), TEST_PORT)).sync();

		messageBus.register(new TestListener());
	}

	static class TestListener {

		@Subscribe
		public void messageReceived(Message message) {
			echo = message;

			synchronized (stack) {
				stack.notify();
			}
		}
	}

	@AfterClass
	public static void deinitPipeline() throws InterruptedException {
		stack.shutdown();
	}
}
