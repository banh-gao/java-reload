package com.github.reload.net;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import java.net.InetSocketAddress;
import com.github.reload.Context;
import com.github.reload.net.data.FramedMessageCodec;
import com.github.reload.net.data.HeadedMessageDecoder;
import com.github.reload.net.data.Message;
import com.github.reload.net.data.MessageDecoder;
import com.github.reload.net.data.MessageEncoder;
import com.github.reload.net.handlers.ForwardingHandler;
import com.github.reload.net.handlers.MessageHandler;
import com.github.reload.net.handlers.SRLinkHandler;

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

	private final MessageHandler msgHandler;

	public ChannelInitializerImpl(MessageHandler msgHandler) {
		// TODO: share codec instances between channels
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
		pipeline.addLast(FWD_DECODER, new HeadedMessageDecoder(new Context()));

		// IN: Forward to other links the messages not directed to this node
		// OUT: Forward on this link the messages not directed to this node
		pipeline.addLast(FWD_HANDLER, new ForwardingHandler());

		// IN: Decoder for RELOAD message content and security block, header
		// must have been already decoded at this point
		pipeline.addLast(MSG_DECODER, new MessageDecoder(new Context()));

		// OUT: Encoder for complete RELOAD message
		pipeline.addLast(MSG_ENCODER, new MessageEncoder(new Context()));

		// IN: Process incoming messages directed to this node
		pipeline.addLast(MSG_HANDLER, msgHandler);
	}

	public static void main(String[] args) throws Exception {
		ServerBootstrap sb = new ServerBootstrap();
		sb.group(new NioEventLoopGroup(1)).channel(NioServerSocketChannel.class).childHandler(new ChannelInitializerImpl(new MessageHandler())).childOption(ChannelOption.SO_KEEPALIVE, true);
		ChannelFuture f = sb.bind(6084);

		f.await();

		Bootstrap b = new Bootstrap();
		b.group(new NioEventLoopGroup(1)).channel(NioSocketChannel.class).handler(new ChannelInitializerImpl(new MessageHandler()));
		ChannelFuture f2 = b.connect(new InetSocketAddress(6084));

		f2.await();

		if (!f2.isSuccess())
			f2.cause().printStackTrace();

		ChannelFuture f3 = f2.channel().write(new Message());

		f3.await();

		if (!f3.isSuccess()) {
			f3.cause().printStackTrace();
		}

		System.out.println(f3.isSuccess());
	}
}
