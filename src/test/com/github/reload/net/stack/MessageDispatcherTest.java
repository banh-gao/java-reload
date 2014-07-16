package com.github.reload.net.stack;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import java.net.InetAddress;
import java.util.ArrayList;
import org.junit.Assert;
import org.junit.Test;
import com.github.reload.Configuration;
import com.github.reload.MessageBus;
import com.github.reload.net.NetworkTest;
import com.github.reload.net.encoders.Message;
import com.github.reload.net.encoders.content.Content;
import com.github.reload.net.encoders.content.PingRequest;
import com.github.reload.net.encoders.header.Header;
import com.github.reload.net.encoders.secBlock.GenericCertificate;
import com.github.reload.net.encoders.secBlock.SecurityBlock;
import com.github.reload.net.encoders.secBlock.Signature;
import com.github.reload.net.ice.IceCandidate.OverlayLinkType;
import com.github.reload.net.stack.ReloadStackInitializer;
import com.google.common.eventbus.Subscribe;

public class MessageDispatcherTest extends NetworkTest {

	private Message answer;
	private Channel ch;

	@Test
	public void testDispatch() throws Exception {
		Configuration conf = new Configuration();
		conf.setOverlayAttribute(Configuration.NODE_ID_LENGTH, 5);
		MessageBus messageBus = new MessageBus();
		ReloadStackInitializer initializer = new ReloadStackInitializer(OverlayLinkType.TLS_TCP_FH_NO_ICE, conf, messageBus);

		EventLoopGroup workerGroup = new NioEventLoopGroup();
		Bootstrap b = new Bootstrap();
		b.group(workerGroup);
		b.channel(NioSocketChannel.class);
		b.option(ChannelOption.SO_KEEPALIVE, true);
		b.handler(initializer);

		ch = b.connect(InetAddress.getLocalHost(), TEST_PORT).sync().channel();

		messageBus.register(new TestListener());

		Header h = new Header();
		Content content = new PingRequest(75);
		SecurityBlock s = new SecurityBlock(new ArrayList<GenericCertificate>(), Signature.EMPTY_SIGNATURE);

		Message message = new Message(h, content, s);

		ch.writeAndFlush(message);

		synchronized (ch) {
			ch.wait(1000);
		}

		Assert.assertNotNull(answer);

		Assert.assertEquals(message.getHeader().getTransactionId(), answer.getHeader().getTransactionId());

		ch.close();
	}

	class TestListener {

		@Subscribe
		public void messageReceived(Message message) {
			answer = message;
			synchronized (ch) {
				ch.notify();
			}
		}
	}
}