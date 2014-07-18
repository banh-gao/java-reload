package com.github.reload.net;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import java.net.InetAddress;
import java.util.List;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import com.github.reload.net.encoders.header.NodeID;

public class NetworkTest {

	public static int TEST_PORT = 6084;
	public static NodeID TEST_NODEID = NodeID.valueOf(new byte[]{1, 2, 3, 4, 5,
																	6, 7, 8, 9,
																	10, 11, 12,
																	13, 14, 15,
																	16});

	private static EventLoopGroup workerGroup = new NioEventLoopGroup();

	@BeforeClass
	public static void runEchoServer() throws Exception {
		ServerBootstrap b = new ServerBootstrap();
		b.group(workerGroup).channel(NioServerSocketChannel.class).childHandler(new ChannelInitializer<SocketChannel>() {

			@Override
			public void initChannel(SocketChannel ch) throws Exception {
				ch.pipeline().addLast(new SimpleChannelInboundHandler<ByteBuf>() {

					@Override
					protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
						msg.retain();
						ctx.writeAndFlush(msg);

					}

					@Override
					public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
						cause.printStackTrace();
					}
				});

			}
		}).option(ChannelOption.SO_BACKLOG, 128).childOption(ChannelOption.SO_KEEPALIVE, true);

		// Bind and start to accept incoming connections.
		b.bind(TEST_PORT).sync();
	}

	@AfterClass
	public static void shutdownServer() throws InterruptedException {
		workerGroup.shutdownGracefully();
	}

	public static MsgTester initTester(final List<? extends ChannelHandler> handlers) throws Exception {
		EventLoopGroup workerGroup = new NioEventLoopGroup();
		Bootstrap b = new Bootstrap();
		b.group(workerGroup);
		b.channel(NioSocketChannel.class);
		b.option(ChannelOption.SO_KEEPALIVE, true);
		final MsgTester tester = new MsgTester();
		b.handler(new ChannelInitializer<SocketChannel>() {

			@Override
			public void initChannel(SocketChannel ch) throws Exception {

				tester.ch = ch;
				for (ChannelHandler h : handlers) {
					ch.pipeline().addLast("Handler_" + h.getClass().getCanonicalName(), h);
				}

				ch.pipeline().addLast("TesterHandler", new SimpleChannelInboundHandler<Object>() {

					@Override
					protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
						synchronized (tester) {
							tester.echo = msg;
							tester.notifyAll();
						}
					}

					@Override
					public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
						synchronized (tester) {
							tester.exception = cause;
							tester.notifyAll();
						}
					}
				});
			}
		});

		b.connect(InetAddress.getLocalHost(), TEST_PORT).sync();
		return tester;
	}

	public static class MsgTester {

		protected Throwable exception;
		private Channel ch;
		private Object echo;

		public Channel getChannel() {
			return ch;
		}

		public Object sendMessage(Object message) throws Exception {
			ch.writeAndFlush(message).sync();

			synchronized (this) {
				wait();
			}

			if (exception != null)
				throw new Exception(exception);

			return echo;
		}

		public void shutdown() throws InterruptedException {
			ch.closeFuture();
		}
	}
}
