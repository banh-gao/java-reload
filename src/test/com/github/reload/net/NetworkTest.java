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
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.net.ssl.SSLEngine;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import com.github.reload.Components.Component;
import com.github.reload.crypto.CryptoHelper;
import com.github.reload.crypto.Keystore;
import com.github.reload.crypto.ReloadCertificate;
import com.github.reload.crypto.ReloadCertificateParser;
import com.github.reload.net.encoders.header.NodeID;
import com.github.reload.net.encoders.header.RoutableID;
import com.github.reload.net.encoders.secBlock.HashAlgorithm;
import com.github.reload.net.encoders.secBlock.SignatureAlgorithm;
import com.github.reload.net.encoders.secBlock.SignerIdentity;
import com.github.reload.net.ice.IceCandidate.OverlayLinkType;
import com.github.reload.routing.RoutingTable;

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

	@Component(RoutingTable.class)
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

	@Component(CryptoHelper.class)
	public static class TestCrypto extends CryptoHelper {

		@Override
		public HashAlgorithm getSignHashAlg() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public SignatureAlgorithm getSignAlg() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public HashAlgorithm getCertHashAlg() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public boolean belongsTo(ReloadCertificate certificate, SignerIdentity identity) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public ReloadCertificateParser getCertificateParser() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public List<? extends Certificate> getTrustRelationship(Certificate peerCert, Certificate trustedIssuer, List<? extends Certificate> availableCerts) throws CertificateException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public SSLEngine getSSLEngine(OverlayLinkType linkType) throws NoSuchAlgorithmException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		protected Keystore getKeystore() {
			// TODO Auto-generated method stub
			return null;
		}

	}
}
