package com.github.reload.net.pipeline;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.ssl.SslHandler;
import javax.net.ssl.SSLEngine;
import com.github.reload.Configuration;
import com.github.reload.Context;
import com.github.reload.InitializationException;
import com.github.reload.crypto.CryptoHelper;
import com.github.reload.net.MessageRouter;
import com.github.reload.net.ice.IceCandidate.OverlayLinkType;
import com.github.reload.net.pipeline.encoders.FramedMessageCodecTest;
import com.github.reload.net.pipeline.encoders.HeadedMessageDecoder;
import com.github.reload.net.pipeline.encoders.MessageDecoderTest;
import com.github.reload.net.pipeline.encoders.MessageEncoderTest;
import com.github.reload.net.pipeline.handlers.ForwardingHandler;
import com.github.reload.net.pipeline.handlers.LinkHandler;
import com.github.reload.net.pipeline.handlers.MessageDispatcherTest;
import com.github.reload.net.pipeline.handlers.SRLinkHandler;

/**
 * Initialize an newly created channel
 */
public class ChannelInitializerImpl extends ChannelInitializer<Channel> {

	private static final String FRAME_CODEC = "FRM_CODEC";
	private static final String FWD_DECODER = "FWD_DECODER";
	private static final String MSG_DECODER = "MSG_DECODER";
	private static final String MSG_ENCODER = "MSG_ENCODER";

	private static final String CRYPTO_HANDLER = "CRYPTO_HANDLER";
	private static final String LINK_HANDLER = "LINK_HANDLER";
	private static final String MSG_HANDLER = "MSG_HANDLER";

	private final Context context;
	private final OverlayLinkType linkType;

	public ChannelInitializerImpl(Context context, OverlayLinkType linkType) {
		this.context = context;
		this.linkType = linkType;
	}

	@Override
	protected void initChannel(Channel ch) throws Exception {
		initPipeline(ch.pipeline());
	}

	private void initPipeline(ChannelPipeline pipeline) throws InitializationException {
		Configuration conf = context.getComponent(Configuration.class);

		// IN/OUT: add encrypted tunneling layer
		addCryptoLayer(pipeline);

		// IN/OUT: Codec for RELOAD framing message
		pipeline.addLast(FRAME_CODEC, new FramedMessageCodecTest());

		// IN/OUT: Specific link handler to control link reliability
		addLinkLayer(pipeline);

		// IN: Decoder for RELOAD forwarding header
		pipeline.addLast(FWD_DECODER, new HeadedMessageDecoder(conf));

		ForwardingHandler fwdHandler = new ForwardingHandler(context.getComponent(MessageRouter.class));
		// IN: Forward to other links the messages not directed to this node
		// OUT: Forward on this link the messages not directed to this node
		pipeline.addLast(ForwardingHandler.NAME, fwdHandler);

		// IN: Decoder for RELOAD message content and security block, header
		// must have been already decoded at this point
		pipeline.addLast(MSG_DECODER, new MessageDecoderTest(conf));

		// OUT: Encoder for complete RELOAD message
		pipeline.addLast(MSG_ENCODER, new MessageEncoderTest(conf));

		MessageDispatcherTest msgReceiver = new MessageDispatcherTest(context.getMessageBus());
		// IN: Process incoming messages directed to this node
		pipeline.addLast(MSG_HANDLER, msgReceiver);
	}

	private void addCryptoLayer(ChannelPipeline pipeline) {
		SSLEngine sslEngine = context.getComponent(CryptoHelper.class).getClientSSLEngine(linkType);

		pipeline.addLast(CRYPTO_HANDLER, new SslHandler(sslEngine, false));
	}

	private void addLinkLayer(ChannelPipeline pipeline) throws InitializationException {
		LinkHandler linkHandler;
		switch (linkType) {
			case DTLS_UDP_SR :
			case DTLS_UDP_SR_NO_ICE :
				linkHandler = new SRLinkHandler();
				break;
			default :
				throw new InitializationException("No valid link protocol implementation available");
		}
		pipeline.addLast(LINK_HANDLER, linkHandler);
	}
}
