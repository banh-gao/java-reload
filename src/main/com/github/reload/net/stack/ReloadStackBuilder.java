package com.github.reload.net.stack;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslHandler;
import java.net.InetSocketAddress;
import java.security.NoSuchAlgorithmException;
import javax.net.ssl.SSLEngine;
import com.github.reload.Configuration;
import com.github.reload.net.encoders.ForwardMessageCodec;
import com.github.reload.net.encoders.FramedMessageCodec;
import com.github.reload.net.encoders.MessageCodec;
import com.github.reload.net.ice.IceCandidate.OverlayLinkType;

public class ReloadStackBuilder {

	private final Configuration conf;
	private final MessageDispatcher msgDispatcher;

	private Bootstrap bootstrap = new Bootstrap();
	private SslHandler sslHandler;
	private LinkHandler linkHandler;
	private InetSocketAddress localAddress;

	public ReloadStackBuilder(Configuration conf, MessageDispatcher msgDispatcher) {
		this.conf = conf;
		this.msgDispatcher = msgDispatcher;
		EventLoopGroup workerGroup = new NioEventLoopGroup();
		bootstrap.group(workerGroup);
		bootstrap.channel(NioSocketChannel.class);
		bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
	}

	public void setSslEngine(SSLEngine sslEngine) {
		this.sslHandler = new SslHandler(sslEngine);
	}

	public void setLinkType(OverlayLinkType linkType) throws NoSuchAlgorithmException {
		this.linkHandler = LinkHandlerFactory.getInstance(linkType);
	}

	public void setLocalAddress(InetSocketAddress localAddress) {
		this.localAddress = localAddress;
	}

	public ReloadStack buildStack() throws InterruptedException {
		initPipeline();

		if (localAddress == null)
			localAddress = new InetSocketAddress(0);

		return new ReloadStack(bootstrap.bind(localAddress).sync().channel());
	}

	private void initPipeline() {
		if (linkHandler == null)
			throw new IllegalStateException();

		bootstrap.handler(new ChannelInitializer<Channel>() {

			@Override
			protected void initChannel(Channel ch) throws Exception {

				ChannelPipeline pipeline = ch.pipeline();

				// Encrypted tunnel handler
				if (sslHandler != null)
					pipeline.addLast(ReloadStack.HANDLER_SSL, sslHandler);

				// Codec for RELOAD framing message
				pipeline.addLast(ReloadStack.CODEC_FRAME, new FramedMessageCodec());

				// Link handler to manage link reliability
				pipeline.addLast(ReloadStack.HANDLER_LINK, linkHandler);

				// Codec for RELOAD forwarding header
				pipeline.addLast(ReloadStack.CODEC_FORWARD, new ForwardMessageCodec(conf));

				// Decides whether an incoming message has to be processed
				// locally or forwarded to a neighbor node
				pipeline.addLast(ReloadStack.HANDLER_FORWARD, new ForwardingHandler());

				// Codec for message payload (content + security block)
				pipeline.addLast(ReloadStack.CODEC_MESSAGE, new MessageCodec(conf));

				// Dispatch incoming messages on the application message bus
				pipeline.addLast(ReloadStack.HANDLER_DISPATCHER, msgDispatcher);
			}
		});
	}
}