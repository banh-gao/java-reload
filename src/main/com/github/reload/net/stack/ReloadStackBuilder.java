package com.github.reload.net.stack;

import io.netty.bootstrap.AbstractBootstrap;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslHandler;
import java.net.InetSocketAddress;
import java.security.NoSuchAlgorithmException;
import javax.net.ssl.SSLEngine;
import com.github.reload.Components;
import com.github.reload.conf.Configuration;
import com.github.reload.crypto.CryptoHelper;
import com.github.reload.net.encoders.ForwardMessageCodec;
import com.github.reload.net.encoders.FramedMessageCodec;
import com.github.reload.net.encoders.MessageCodec;
import com.github.reload.net.ice.IceCandidate.OverlayLinkType;

public class ReloadStackBuilder {

	private final boolean isServer;
	private final Configuration conf;
	private final MessageDispatcher msgDispatcher;

	private final AbstractBootstrap<?, ?> bootstrap;
	private final CryptoHelper<?> cryptoHelper;
	private InetSocketAddress localAddress;
	private OverlayLinkType linkType;
	private ForwardingHandler fwdHandler;

	public static ReloadStackBuilder newClientBuilder(MessageDispatcher msgDispatcher) {
		Bootstrap b = new Bootstrap();
		b.channel(NioSocketChannel.class);
		return new ReloadStackBuilder(msgDispatcher, b, false);
	}

	public static ReloadStackBuilder newServerBuilder(MessageDispatcher msgDispatcher, final ChannelHandler... extraHandlers) {
		final ServerBootstrap b = new ServerBootstrap();
		b.channel(NioServerSocketChannel.class);

		return new ReloadStackBuilder(msgDispatcher, b, true) {

			@Override
			protected void initPipeline() {
				b.childHandler(newInitializer(extraHandlers));
			}
		};
	}

	protected <T extends AbstractBootstrap<T, ? extends Channel>> ReloadStackBuilder(MessageDispatcher msgDispatcher, T bootstrap, boolean isServer) {
		this.isServer = isServer;
		this.fwdHandler = new ForwardingHandler();
		this.bootstrap = bootstrap;
		this.conf = (Configuration) Components.get(Configuration.COMPNAME);
		this.cryptoHelper = (CryptoHelper<?>) Components.get(CryptoHelper.COMPNAME);
		this.msgDispatcher = msgDispatcher;
		EventLoopGroup workerGroup = new NioEventLoopGroup();
		bootstrap.group(workerGroup);
		bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
	}

	public void setLinkType(OverlayLinkType linkType) throws NoSuchAlgorithmException {
		this.linkType = linkType;
	}

	public void setLocalAddress(InetSocketAddress localAddress) {
		this.localAddress = localAddress;
	}

	public ReloadStack buildStack() throws InterruptedException {
		if (linkType == null)
			throw new IllegalStateException();

		initPipeline();

		if (localAddress == null)
			localAddress = new InetSocketAddress(0);

		return new ReloadStack(bootstrap.bind(localAddress).sync().channel());
	}

	protected void initPipeline() {
		bootstrap.handler(newInitializer());
	}

	protected ChannelInitializer<Channel> newInitializer(final ChannelHandler... extraHandlers) {
		return new ChannelInitializer<Channel>() {

			@Override
			protected void initChannel(Channel ch) throws Exception {
				ChannelPipeline pipeline = ch.pipeline();

				// Encrypted tunnel handler
				SSLEngine eng = cryptoHelper.newSSLEngine(linkType);
				if (isServer) {
					eng.setNeedClientAuth(true);
					eng.setUseClientMode(false);
				} else {
					eng.setUseClientMode(true);
				}

				eng.setEnabledProtocols(new String[]{"SSLv3"});

				pipeline.addLast(ReloadStack.HANDLER_SSL, new SslHandler(eng));

				// Codec for RELOAD framing message
				pipeline.addLast(ReloadStack.CODEC_FRAME, new FramedMessageCodec());

				// Link handler to manage link reliability
				pipeline.addLast(ReloadStack.HANDLER_LINK, LinkHandlerFactory.getInstance(linkType));

				// Codec for RELOAD forwarding header
				pipeline.addLast(ReloadStack.CODEC_HEADER, new ForwardMessageCodec(conf));

				// Decides whether an incoming message has to be processed
				// locally or forwarded to a neighbor node
				pipeline.addLast(ReloadStack.HANDLER_FORWARD, fwdHandler);

				// Codec for message payload (content + security block)
				pipeline.addLast(ReloadStack.CODEC_PAYLOAD, new MessageCodec(conf));

				// TODO: insert message authentication layer

				// Dispatch incoming messages on the application message bus
				pipeline.addLast(ReloadStack.HANDLER_DISPATCHER, msgDispatcher);

				pipeline.addLast(extraHandlers);
			}
		};
	}
}