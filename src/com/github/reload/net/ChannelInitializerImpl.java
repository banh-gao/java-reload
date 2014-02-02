package com.github.reload.net;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import com.github.reload.net.data.ForwardMessageCodec;
import com.github.reload.net.data.FrameMessageCodec;
import com.github.reload.net.data.MessageCodec;
import com.github.reload.net.handlers.ForwardingHandler;
import com.github.reload.net.handlers.LinkHandler;
import com.github.reload.net.handlers.MessageHandler;

/**
 * Initialize an newly created channel
 */
public class ChannelInitializerImpl extends ChannelInitializer<Channel> {

	public static final String FRAME_CODEC = "FRAME_CODEC";
	public static final String LINK_HANDLER = "LINK_HANDLER";
	public static final String FWD_CODEC = "FWD_CODEC";
	public static final String FWD_HANDLER = "FWD_HANDLER";
	public static final String MSG_CODEC = "MSG_CODEC";
	public static final String MSG_HANDLER = "MSG_HANDLER";

	private final LinkHandler linkHandler;
	private final ForwardingHandler fwdHandler;
	private final MessageHandler msgHandler;

	public ChannelInitializerImpl(LinkHandler linkHandler, ForwardingHandler fwdHandler, MessageHandler msgHandler) {
		// TODO: share codecs instances between channels
		this.linkHandler = linkHandler;
		this.fwdHandler = fwdHandler;
		this.msgHandler = msgHandler;
	}

	@Override
	protected void initChannel(Channel ch) throws Exception {
		initPipeline(ch.pipeline());
	}

	private void initPipeline(ChannelPipeline pipeline) {
		// IN/OUT: Codec for RELOAD framing message
		pipeline.addLast(FRAME_CODEC, new FrameMessageCodec());

		// IN/OUT: Specific link handler to control link reliability
		pipeline.addLast(LINK_HANDLER, linkHandler);

		// IN/OUT: Codec for RELOAD forwarding header
		pipeline.addLast(FWD_CODEC, new ForwardMessageCodec());

		// IN: check header and pass messages directly to the forwarding handler
		// of the channel where the message has to be routed
		pipeline.addLast(FWD_HANDLER, fwdHandler);

		// IN/OUT: Codec for RELOAD message content and security block
		pipeline.addLast(MSG_CODEC, new MessageCodec());

		// IN: Process incoming messages directed to this node
		pipeline.addLast(MSG_HANDLER, msgHandler);
	}

}
