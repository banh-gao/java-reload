package com.github.reload.net.stack;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import java.net.InetAddress;
import java.util.ArrayList;
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

public class ReloadStackInitializerTest extends NetworkTest {

	@Test
	public void test() throws Exception {
		Configuration conf = new Configuration();
		MessageBus messageBus = new MessageBus();
		ReloadStackInitializer initializer = new ReloadStackInitializer(OverlayLinkType.TLS_TCP_FH_NO_ICE, conf, messageBus);

		EventLoopGroup workerGroup = new NioEventLoopGroup();
		Bootstrap b = new Bootstrap();
		b.group(workerGroup);
		b.channel(NioSocketChannel.class);
		b.option(ChannelOption.SO_KEEPALIVE, true);
		b.handler(initializer);

		Channel ch = b.connect(InetAddress.getLocalHost(), TEST_PORT).sync().channel();

		messageBus.register(new TestListener());

		Header h = new Header();
		Content content = new PingRequest(75);
		SecurityBlock s = new SecurityBlock(new ArrayList<GenericCertificate>(), Signature.EMPTY_SIGNATURE);

		Message message = new Message(h, content, s);

		ch.writeAndFlush(message);

		Thread.sleep(1000);
	}

	class TestListener {

		@Subscribe
		public void messageReceived(Message message) {
			System.out.println(message);
		}
	}
}
