package com.github.reload.net.connections;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import com.github.reload.Configuration;
import com.github.reload.Context;
import com.github.reload.net.ForwardingHandler;
import com.github.reload.net.MessageDispatcher;
import com.github.reload.net.data.FramedMessageCodec;
import com.github.reload.net.data.HeadedMessageDecoder;
import com.github.reload.net.data.MessageDecoder;
import com.github.reload.net.data.MessageEncoder;
import com.github.reload.net.ice.IceCandidate.OverlayLinkType;

/**
 * Initialize an newly created channel
 */
public class ChannelInitializerImpl extends ChannelInitializer<Channel> {

	public static final String FRAME_CODEC = "FRM_CODEC";
	public static final String FWD_DECODER = "FWD_DECODER";
	public static final String MSG_DECODER = "MSG_DECODER";
	public static final String MSG_ENCODER = "MSG_ENCODER";

	public static final String LINK_HANDLER = "LINK_HANDLER";
	public static final String FWD_HANDLER = "FWD_HANDLER";
	public static final String MSG_HANDLER = "MSG_HANDLER";

	private final ForwardingHandler fwdHandler;
	private final MessageDispatcher msgHandler;

	public ChannelInitializerImpl(Context context, OverlayLinkType linkType) {
		// TODO: share codec instances between channels
		this.fwdHandler = fwdHandler;
		this.msgHandler = msgHandler;
	}

	@Override
	protected void initChannel(Channel ch) throws Exception {
		initPipeline(ch.pipeline());
	}

	private void initPipeline(ChannelPipeline pipeline) {
		// IN/OUT: Codec for RELOAD framing message
		pipeline.addLast(FRAME_CODEC, new FramedMessageCodec());

		// IN/OUT: Specific link handler to control link reliability
		pipeline.addLast(LINK_HANDLER, new SRLinkHandler());

		// IN: Decoder for RELOAD forwarding header
		pipeline.addLast(FWD_DECODER, new HeadedMessageDecoder(new Configuration()));

		// IN: Forward to other links the messages not directed to this node
		// OUT: Forward on this link the messages not directed to this node
		pipeline.addLast(FWD_HANDLER, fwdHandler);

		// IN: Decoder for RELOAD message content and security block, header
		// must have been already decoded at this point
		pipeline.addLast(MSG_DECODER, new MessageDecoder(new Configuration()));

		// OUT: Encoder for complete RELOAD message
		pipeline.addLast(MSG_ENCODER, new MessageEncoder(new Configuration()));

		// IN: Process incoming messages directed to this node
		pipeline.addLast(MSG_HANDLER, msgHandler);
	}
}
