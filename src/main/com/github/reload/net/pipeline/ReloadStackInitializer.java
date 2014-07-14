package com.github.reload.net.pipeline;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.ssl.SslHandler;
import java.security.NoSuchAlgorithmException;
import javax.net.ssl.SSLEngine;
import com.github.reload.Configuration;
import com.github.reload.InitializationException;
import com.github.reload.MessageBus;
import com.github.reload.crypto.CryptoHelper;
import com.github.reload.net.ice.IceCandidate.OverlayLinkType;
import com.github.reload.net.pipeline.encoders.FramedMessageCodec;
import com.github.reload.net.pipeline.encoders.HeadedMessageDecoder;
import com.github.reload.net.pipeline.encoders.PayloadDecoder;
import com.github.reload.net.pipeline.encoders.MessageEncoder;
import com.github.reload.net.pipeline.handlers.ForwardingHandler;
import com.github.reload.net.pipeline.handlers.LinkHandler;
import com.github.reload.net.pipeline.handlers.LinkHandlerFactory;
import com.github.reload.net.pipeline.handlers.MessageDispatcher;

/**
 * Initialize a RELOAD stack for a specific connection type
 */
public class ReloadStackInitializer extends ChannelInitializer<Channel> {

	private static final String FRAME_CODEC = "FRM_CODEC";
	private static final String FWD_DECODER = "FWD_DECODER";
	private static final String MSG_DECODER = "MSG_DECODER";
	private static final String MSG_ENCODER = "MSG_ENCODER";

	private static final String CRYPTO_HANDLER = "CRYPTO_HANDLER";
	private static final String LINK_HANDLER = "LINK_HANDLER";
	public static final String FWD_HANDLER = "FWD_HANDLER";
	private static final String MSG_HANDLER = "MSG_HANDLER";

	private final OverlayLinkType linkType;
	private final Configuration conf;

	private final LinkHandler linkHandler;
	private final ForwardingHandler fwdHandler;

	private SslHandler sslHandler;
	private MessageBus messageBus;

	public ReloadStackInitializer(OverlayLinkType linkType, Configuration conf, MessageBus messageBus) throws NoSuchAlgorithmException {
		this.linkType = linkType;
		this.linkHandler = LinkHandlerFactory.getInstance(linkType);
		this.fwdHandler = new ForwardingHandler();
		this.conf = conf;
		this.messageBus = messageBus;
	}

	@Override
	protected void initChannel(Channel ch) throws Exception {
		initPipeline(ch.pipeline());
	}

	private void initPipeline(ChannelPipeline pipeline) throws InitializationException {
		// IN/OUT: add encrypted tunneling layer
		if (sslHandler != null)
			pipeline.addLast(CRYPTO_HANDLER, sslHandler);

		// IN/OUT: Codec for RELOAD framing message
		pipeline.addLast(FRAME_CODEC, new FramedMessageCodec());

		// IN/OUT: Specific link handler to control link reliability
		pipeline.addLast(LINK_HANDLER, linkHandler);

		// IN: Decoder for RELOAD forwarding header
		pipeline.addLast(FWD_DECODER, new HeadedMessageDecoder(conf));

		// IN: Forward to other links the messages not directed to this node
		// OUT: Forward on this link the messages not directed to this node
		pipeline.addLast(FWD_HANDLER, fwdHandler);

		// IN: Decoder for RELOAD message content and security block, header
		// must have been already decoded at this point
		pipeline.addLast(MSG_DECODER, new PayloadDecoder(conf));

		// OUT: Encoder for complete RELOAD message
		pipeline.addLast(MSG_ENCODER, new MessageEncoder(conf));

		// IN: Dispatch incoming messages on the application message bus
		pipeline.addLast(MSG_HANDLER, new MessageDispatcher(messageBus));
	}

	public void setCryptoLayer(CryptoHelper cryptoHelper) throws NoSuchAlgorithmException {
		SSLEngine sslEngine = cryptoHelper.getClientSSLEngine(linkType);
		sslHandler = new SslHandler(sslEngine, false);
	}

	public ForwardingHandler getForwardingHandler() {
		return fwdHandler;
	}
}
