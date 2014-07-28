package com.github.reload.net.server;

import io.netty.bootstrap.AbstractBootstrap;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import java.net.InetSocketAddress;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import com.github.reload.Components;
import com.github.reload.conf.Configuration;
import com.github.reload.crypto.CryptoHelper;
import com.github.reload.net.encoders.FramedMessageCodec;
import com.github.reload.net.encoders.MessageAuthenticator;
import com.github.reload.net.encoders.MessageEncoder;
import com.github.reload.net.encoders.MessageHeaderDecoder;
import com.github.reload.net.encoders.MessagePayloadDecoder;
import com.github.reload.net.ice.HostCandidate.OverlayLinkType;
import com.github.reload.net.stack.ReloadStack;

public class UDPReloadStackBuilder {

	private final Configuration conf;
	private final ChannelHandler msgHandler;

	private final AbstractBootstrap<?, ?> bootstrap;
	private final CryptoHelper<?> cryptoHelper;
	private InetSocketAddress localAddress;
	private OverlayLinkType linkType;

	public static UDPReloadStackBuilder newClientBuilder(ChannelHandler msgHandler) {
		Bootstrap b = new Bootstrap();
		b.channel(NioDatagramChannel.class);
		return new UDPReloadStackBuilder(msgHandler, b, false);
	}

	public static UDPReloadStackBuilder newServerBuilder(ChannelHandler msgHandler) {
		final Bootstrap b = new Bootstrap();
		b.channel(NioDatagramChannel.class);
		return new UDPReloadStackBuilder(msgHandler, b, true);
	}

	protected <T extends AbstractBootstrap<T, ? extends Channel>> UDPReloadStackBuilder(ChannelHandler msgHandler, T bootstrap, boolean isServer) {
		this.bootstrap = bootstrap;
		this.conf = (Configuration) Components.get(Configuration.COMPNAME);
		this.cryptoHelper = (CryptoHelper<?>) Components.get(CryptoHelper.COMPNAME);
		this.msgHandler = msgHandler;
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

		DatagramChannel ch = (DatagramChannel) bootstrap.bind(localAddress).sync().channel();

		if (localAddress.getAddress().isMulticastAddress())
			ch.joinGroup(localAddress.getAddress());

		return new ReloadStack(ch);
	}

	protected void initPipeline() {
		bootstrap.handler(newInitializer());
	}

	protected ChannelInitializer<Channel> newInitializer(final ChannelHandler... extraHandlers) {
		return new ChannelInitializer<Channel>() {

			@SuppressWarnings("unchecked")
			@Override
			protected void initChannel(Channel ch) throws Exception {
				ChannelPipeline pipeline = ch.pipeline();

				// Codec for RELOAD framing message
				pipeline.addLast(ReloadStack.CODEC_FRAME, new FramedMessageCodec());

				// Codec for RELOAD forwarding header
				pipeline.addLast(ReloadStack.DECODER_HEADER, new MessageHeaderDecoder(conf));

				// Decoder for message payload (content + security block)
				pipeline.addLast(ReloadStack.DECODER_PAYLOAD, new MessagePayloadDecoder(conf));

				pipeline.addLast(ReloadStack.HANDLER_MESSAGE, new MessageAuthenticator((CryptoHelper<Certificate>) cryptoHelper));

				// Encorder for message entire outgoing message, also
				// responsible for message signature generation
				pipeline.addLast(ReloadStack.ENCODER_MESSAGE, new MessageEncoder(conf));

				// Dispatch incoming messages on the application message bus
				pipeline.addLast(ReloadStack.HANDLER_DISPATCHER, msgHandler);

				pipeline.addLast(extraHandlers);
			}
		};
	}
}