package com.github.reload.net.stack;

import io.netty.bootstrap.AbstractBootstrap;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
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
import io.netty.util.AttributeKey;
import java.net.InetSocketAddress;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
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
	StackInitializer stackInitializer;

	private final AbstractBootstrap<?, ?> bootstrap;
	private InetSocketAddress localAddress;

	static final AttributeKey<Boolean> ATTR_SERVER = AttributeKey.valueOf("SERVER");
	private final boolean isServer;

	static final AttributeKey<OverlayLinkType> ATTR_LINKTYPE = AttributeKey.valueOf("LINKTYPE");
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

	private ReloadStackBuilder(boolean isServer) {
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

		if (isServer) {
			ServerBootstrap sBoot = (ServerBootstrap) bootstrap;
			sBoot.childAttr(ATTR_SERVER, isServer);
			sBoot.childAttr(ATTR_LINKTYPE, linkType);
			sBoot.childHandler(stackInitializer);
		} else {
			bootstrap.attr(ATTR_SERVER, isServer);
			bootstrap.attr(ATTR_LINKTYPE, linkType);
			bootstrap.handler(stackInitializer);
		}

		if (localAddress == null) {
			localAddress = new InetSocketAddress(0);
		}

		try {
			return new ReloadStack(bootstrap.bind(localAddress).sync().channel());
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	@Singleton
	@Sharable
	public static class StackInitializer extends ChannelInitializer<Channel> {

		@Inject
		ObjectGraph g;

		@Inject
		CryptoHelper cryptoHelper;

		@Inject
		ConnectionManager connMgr;

		@Inject
		Provider<FramedMessageCodec> frameCodec;
		@Inject
		Provider<MessageHeaderDecoder> msgHdrDec;
		@Inject
		Provider<ForwardingHandler> fwdHandler;
		@Inject
		Provider<MessagePayloadDecoder> msgPayDec;
		@Inject
		Provider<MessageAuthenticator> msgAuth;
		@Inject
		Provider<MessageEncoder> msgEncoder;
		@Inject
		Provider<MessageDispatcher> msgDispatcher;

		@Override
		protected void initChannel(Channel ch) throws Exception {
			boolean isServer = ch.attr(ReloadStackBuilder.ATTR_SERVER).get();
			OverlayLinkType linkType = ch.attr(ReloadStackBuilder.ATTR_LINKTYPE).get();

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
			pipeline.addLast(ReloadStack.CODEC_FRAME, frameCodec.get());

			// Link handler to manage link reliability
			pipeline.addLast(ReloadStack.HANDLER_LINK, g.get(linkType.getHandler()));

			// Codec for RELOAD forwarding header
			pipeline.addLast(ReloadStack.DECODER_HEADER, msgHdrDec.get());

			// Decides whether an incoming message has to be processed
			// locally or forwarded to a neighbor node
			pipeline.addLast(ReloadStack.HANDLER_FORWARD, fwdHandler.get());

			// Decoder for message payload (content + security block)
			pipeline.addLast(ReloadStack.DECODER_PAYLOAD, msgPayDec.get());

			pipeline.addLast(ReloadStack.HANDLER_MESSAGE, msgAuth.get());

			// Encorder for message entire outgoing message, also
			// responsible for message signature generation
			pipeline.addLast(ReloadStack.ENCODER_MESSAGE, msgEncoder.get());

			// Dispatch incoming messages on the application message bus
			pipeline.addLast(ReloadStack.HANDLER_DISPATCHER, msgDispatcher.get());

			if (isServer)
				pipeline.addLast(new ServerStatusHandler());
		}

		@Sharable
		private class ServerStatusHandler extends ChannelInboundHandlerAdapter {

			@Override
			public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
				connMgr.remoteNodeAccepted(ctx.channel());
				super.channelRegistered(ctx);
			}
		}
	}
}