package com.github.reload.net;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import com.github.reload.net.data.FramedMessageCodec;
import com.github.reload.net.data.HeadedMessageDecoder;
import com.github.reload.net.data.MessageDecoder;
import com.github.reload.net.data.MessageEncoder;
import com.github.reload.net.handlers.ForwardingHandler;
import com.github.reload.net.handlers.LinkHandler;
import com.github.reload.net.handlers.MessageHandler;

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
		pipeline.addLast(FRAME_CODEC, new FramedMessageCodec());

		// IN/OUT: Specific link handler to control link reliability
		pipeline.addLast(LINK_HANDLER, linkHandler);

		// IN: Decoder for RELOAD forwarding header
		pipeline.addLast(FWD_DECODER, new HeadedMessageDecoder());

		// IN: If message is directed to this node pass to upper layer,
		// otherwise forward
		// OUT: Send a forwarded message on the link (not originated locally)
		pipeline.addLast(FWD_HANDLER, fwdHandler);

		// IN: Codec for RELOAD message content and security block
		pipeline.addLast(MSG_DECODER, new MessageDecoder());

		// OUT: Codec for RELOAD message content and security block
		pipeline.addLast(MSG_ENCODER, new MessageEncoder());

		// IN: Process incoming messages directed to this node
		pipeline.addLast(MSG_HANDLER, msgHandler);
	}

}
