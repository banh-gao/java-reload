package com.github.reload.net;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import java.net.InetSocketAddress;
import junit.framework.TestCase;
import org.junit.After;
import org.junit.Before;
import com.github.reload.message.Content;
import com.github.reload.message.Header;
import com.github.reload.message.SecurityBlock;
import com.github.reload.net.data.Message;
import com.github.reload.net.handlers.MessageHandler;

public abstract class TestCodecs extends TestCase {

	protected Channel serverChannel;
	protected Channel clientChannel;

	private EventLoopGroup serverLoopGroup;
	private EventLoopGroup clientLoopGroup;

	@Before
	public void setUp() throws Exception {
		ServerBootstrap sb = new ServerBootstrap();
		serverLoopGroup = new NioEventLoopGroup(1);
		sb.group(serverLoopGroup).channel(NioServerSocketChannel.class).childHandler(new ChannelInitializerImpl(new MessageHandler())).childOption(ChannelOption.SO_KEEPALIVE, true);
		ChannelFuture f = sb.bind(6084);

		f.await();

		if (!f.isSuccess())
			throw new Exception(f.cause());

		serverChannel = f.channel();

		Bootstrap b = new Bootstrap();
		clientLoopGroup = new NioEventLoopGroup(1);
		b.group(clientLoopGroup).channel(NioSocketChannel.class).handler(new ChannelInitializerImpl(new MessageHandler()));
		ChannelFuture f2 = b.connect(new InetSocketAddress(6084));

		f2.await();

		if (!f2.isSuccess())
			throw new Exception(f2.cause());

		clientChannel = f2.channel();
	}

	@After
	public void tearDown() throws Exception {
		clientLoopGroup.shutdownGracefully();
		serverLoopGroup.shutdownGracefully();
	}

	public void testMessage(Content content) throws Exception {
		Header tstHeader = new Header();
		ChannelFuture f3 = clientChannel.write(new Message(tstHeader, content, new SecurityBlock()));
		clientChannel.flush();
		f3.await();

		if (!f3.isSuccess()) {
			throw new Exception(f3.cause());
		}

		assertTrue(f3.isSuccess());
	}
}
