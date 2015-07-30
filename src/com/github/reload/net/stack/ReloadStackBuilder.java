package com.github.reload.net.stack;

import io.netty.bootstrap.AbstractBootstrap;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslHandler;
import java.net.InetSocketAddress;
import javax.inject.Inject;
import javax.net.ssl.SSLEngine;
import com.github.reload.crypto.CryptoHelper;
import com.github.reload.net.ConnectionManager;
import com.github.reload.net.codecs.FramedMessageCodec;
import com.github.reload.net.codecs.MessageEncoder;
import com.github.reload.net.codecs.MessageHeaderDecoder;
import com.github.reload.net.codecs.MessagePayloadDecoder;
import com.github.reload.net.ice.HostCandidate.OverlayLinkType;
import dagger.ObjectGraph;

public abstract class ReloadStackBuilder {

	@Inject
	ObjectGraph g;

	@Inject
	CryptoHelper cryptoHelper;

	@Inject
	ConnectionManager connMgr;

	@Inject
	FramedMessageCodec frameCodec;
	@Inject
	MessageHeaderDecoder msgHdrDec;
	@Inject
	ForwardingHandler fwdHandler;
	@Inject
	MessagePayloadDecoder msgPayDec;
	@Inject
	MessageAuthenticator msgAuth;
	@Inject
	MessageEncoder msgEncoder;
	@Inject
	MessageDispatcher msgDispatcher;

	private final boolean isServer;
	private final AbstractBootstrap<?, ?> bootstrap;

	private InetSocketAddress localAddress;
	private OverlayLinkType linkType;

	public static class ServerStackBuilder extends ReloadStackBuilder {

		@Inject
		public ServerStackBuilder() {
			super(true);
		}
	}

	public static class ClientStackBuilder extends ReloadStackBuilder {

		@Inject
		public ClientStackBuilder() {
			super(false);
		}

	}

	protected ReloadStackBuilder(boolean isServer) {
		this.isServer = isServer;

		if (isServer) {
			this.bootstrap = new ServerBootstrap();
			((ServerBootstrap) this.bootstrap).channel(NioServerSocketChannel.class);

		} else {
			this.bootstrap = new Bootstrap();
			((Bootstrap) this.bootstrap).channel(NioSocketChannel.class);
		}

		EventLoopGroup workerGroup = new NioEventLoopGroup();
		bootstrap.group(workerGroup);
		bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
	}

	public void setLinkType(OverlayLinkType linkType) {
		this.linkType = linkType;
	}

	public void setLocalAddress(InetSocketAddress localAddress) {
		this.localAddress = localAddress;
	}

	public ReloadStack buildStack() {
		if (linkType == null)
			throw new IllegalStateException();

		bootstrap.handler(newInitializer());

		if (localAddress == null) {
			localAddress = new InetSocketAddress(0);
		}

		try {
			return new ReloadStack(bootstrap.bind(localAddress).sync().channel());
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
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

				pipeline.addLast(ReloadStack.HANDLER_SSL, new SslHandler(eng));

				// Codec for RELOAD framing message
				pipeline.addLast(ReloadStack.CODEC_FRAME, frameCodec);

				// Link handler to manage link reliability
				pipeline.addLast(ReloadStack.HANDLER_LINK, g.get(linkType.getHandler()));

				// Codec for RELOAD forwarding header
				pipeline.addLast(ReloadStack.DECODER_HEADER, msgHdrDec);

				// Decides whether an incoming message has to be processed
				// locally or forwarded to a neighbor node
				pipeline.addLast(ReloadStack.HANDLER_FORWARD, fwdHandler);

				// Decoder for message payload (content + security block)
				pipeline.addLast(ReloadStack.DECODER_PAYLOAD, msgPayDec);

				pipeline.addLast(ReloadStack.HANDLER_MESSAGE, msgAuth);

				// Encorder for message entire outgoing message, also
				// responsible for message signature generation
				pipeline.addLast(ReloadStack.ENCODER_MESSAGE, msgEncoder);

				// Dispatch incoming messages on the application message bus
				pipeline.addLast(ReloadStack.HANDLER_DISPATCHER, msgDispatcher);

				if (isServer)
					pipeline.addLast(new ServerStatusHandler());
			}
		};
	}

	@Sharable
	private class ServerStatusHandler extends ChannelInboundHandlerAdapter {

		@Override
		public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
			// FIXME: use event
			connMgr.remoteNodeAccepted(ctx.channel());
			super.channelRegistered(ctx);
		}
	}
}