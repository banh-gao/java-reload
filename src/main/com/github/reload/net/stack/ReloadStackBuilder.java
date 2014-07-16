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
import com.github.reload.net.MessageBus;
import com.github.reload.net.encoders.FramedMessageCodec;
import com.github.reload.net.encoders.HeadedMessageDecoder;
import com.github.reload.net.encoders.MessageEncoder;
import com.github.reload.net.encoders.PayloadDecoder;
import com.github.reload.net.ice.IceCandidate.OverlayLinkType;

public class ReloadStackBuilder {

	private final Configuration conf;
	private final MessageBus messageBus;

	private Bootstrap bootstrap = new Bootstrap();
	private SslHandler sslHandler;
	private LinkHandler linkHandler;
	private InetSocketAddress localAddress;

	public ReloadStackBuilder(Configuration conf, MessageBus messageBus) {
		this.conf = conf;
		this.messageBus = messageBus;
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

				// IN/OUT: add encrypted tunneling layer
				if (sslHandler != null)
					pipeline.addLast(ReloadStack.CRYPTO_HANDLER, sslHandler);

				// IN/OUT: Codec for RELOAD framing message
				pipeline.addLast(ReloadStack.FRAME_CODEC, new FramedMessageCodec());

				// IN/OUT: Specific link handler to control link reliability
				pipeline.addLast(ReloadStack.LINK_HANDLER, linkHandler);

				// IN: Decoder for RELOAD forwarding header
				pipeline.addLast(ReloadStack.FWD_DECODER, new HeadedMessageDecoder(conf));

				// IN: Forward to other links the messages not directed to
				// this node
				// OUT: Forward on this link the messages not directed to
				// this node
				pipeline.addLast(ReloadStack.FWD_HANDLER, new ForwardingHandler());

				// IN: Decoder for RELOAD message content and security
				// block, header
				// must have been already decoded at this point
				pipeline.addLast(ReloadStack.MSG_DECODER, new PayloadDecoder(conf));

				// OUT: Encoder for complete RELOAD message
				pipeline.addLast(ReloadStack.MSG_ENCODER, new MessageEncoder(conf));

				// IN: Dispatch incoming messages on the application message
				// bus
				pipeline.addLast(ReloadStack.MSG_HANDLER, new MessageDispatcher(messageBus));
			}
		});
	}
}