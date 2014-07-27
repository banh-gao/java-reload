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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Collections;
import java.util.List;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import com.github.reload.Components;
import com.github.reload.Components.Component;
import com.github.reload.ReloadConnector;
import com.github.reload.crypto.CryptoHelper;
import com.github.reload.crypto.MemoryKeystore;
import com.github.reload.crypto.ReloadCertificate;
import com.github.reload.crypto.X509CryptoHelper;
import com.github.reload.net.connections.ConnectionManager;
import com.github.reload.net.encoders.MessageBuilderFactory;
import com.github.reload.net.encoders.header.NodeID;
import com.github.reload.net.encoders.secBlock.GenericCertificate.CertificateType;
import com.github.reload.net.encoders.secBlock.HashAlgorithm;
import com.github.reload.net.encoders.secBlock.SignatureAlgorithm;
import com.github.reload.routing.TopologyPlugin;

public class NetworkTest {

	public static InetSocketAddress ECHO_SERVER_ADDR;
	public static HashAlgorithm TEST_HASH = HashAlgorithm.SHA1;
	public static SignatureAlgorithm TEST_SIGN = SignatureAlgorithm.RSA;
	public static NodeID TEST_NODEID = NodeID.valueOf("f16a536ca4028b661fcb864a075f3871");

	private static EventLoopGroup workerGroup = new NioEventLoopGroup();

	public static InetSocketAddress SERVER_ADDR;

	public static X509Certificate CA_CERT;
	public static ReloadCertificate TEST_CERT;
	public static PrivateKey TEST_KEY;

	public static CryptoHelper<?> TEST_CRYPTO;

	@BeforeClass
	public static void runEchoServer() throws Exception {
		SERVER_ADDR = new InetSocketAddress(InetAddress.getLoopbackAddress(), 6084);
		ECHO_SERVER_ADDR = new InetSocketAddress(InetAddress.getLoopbackAddress(), 5000);

		Components.register(new MessageBuilderFactory());

		TEST_CRYPTO = new X509CryptoHelper(TEST_HASH, TEST_SIGN, TEST_HASH);

		Components.register(TEST_CRYPTO);

		CA_CERT = (X509Certificate) loadLocalCert("CAcert.der");
		TEST_CERT = TEST_CRYPTO.getCertificateParser().parse(loadLocalCert("testCert.der"));
		TEST_KEY = loadPrivateKey("testKey.der", SignatureAlgorithm.RSA);

		Components.register(new MemoryKeystore<X509Certificate>(TEST_CERT, TEST_KEY, Collections.singletonMap(TEST_NODEID, TEST_CERT)));

		TestConfiguration conf = new TestConfiguration();
		conf.rootCerts = Collections.singletonList(CA_CERT);
		conf.instanceName = "testOverlay.com";
		conf.maxMessageSize = 5000;
		conf.initialTTL = 6;
		Components.register(conf);

		ConnectionManager connMgr = new ConnectionManager();

		TestConnector testConn = new TestConnector();
		testConn.setLocalAddress(SERVER_ADDR);

		testConn.setLocalNodeId(TEST_NODEID);

		Components.register(testConn);
		Components.register(connMgr);

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
		b.bind(ECHO_SERVER_ADDR.getPort()).sync();
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

		b.connect(InetAddress.getLocalHost(), ECHO_SERVER_ADDR.getPort()).sync();
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

	@Component(ReloadConnector.COMPNAME)
	public static class TestConnector extends ReloadConnector {

		@Override
		protected byte[] getJoinData() {
			return new byte[0];
		}

		@Override
		protected TopologyPlugin getTopologyPlugin() {
			return null;
		}

		@Override
		protected CertificateType getCertificateType() {
			return CertificateType.X509;
		}

		@Override
		protected CryptoHelper<X509Certificate> getCryptoHelper() {
			return null;
		}

		@Override
		public boolean equals(Object obj) {
			return false;
		}

		@Override
		public int hashCode() {
			return 0;
		}

	}

	public static Certificate loadLocalCert(String localCertPath) throws FileNotFoundException, CertificateException {
		if (localCertPath == null || !new File(localCertPath).exists())
			throw new FileNotFoundException("Overlay certificate file not found at " + localCertPath);

		CertificateFactory certFactory;
		try {
			certFactory = CertificateFactory.getInstance("X.509");
			File overlayCertFile = new File(localCertPath);
			InputStream certStream = new FileInputStream(overlayCertFile);
			Certificate cert = certFactory.generateCertificate(certStream);
			certStream.close();
			return cert;
		} catch (CertificateException | IOException e) {
			throw new CertificateException(e);
		}
	}

	private static PrivateKey loadPrivateKey(String privateKeyPath, SignatureAlgorithm keyAlg) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
		if (privateKeyPath == null || !new File(privateKeyPath).exists())
			throw new FileNotFoundException("Private key file not found at " + privateKeyPath);

		File file = new File(privateKeyPath);
		byte[] privKeyBytes = new byte[(int) file.length()];
		InputStream in = new FileInputStream(file);
		in.read(privKeyBytes);
		in.close();
		KeyFactory keyFactory = KeyFactory.getInstance(keyAlg.toString());
		KeySpec ks = new PKCS8EncodedKeySpec(privKeyBytes);
		return keyFactory.generatePrivate(ks);
	}
}
