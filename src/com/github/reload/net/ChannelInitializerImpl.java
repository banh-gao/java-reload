package com.github.reload.net;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import java.net.InetSocketAddress;
import org.omg.IOP.CodecFactory;
import com.github.reload.message.Message;
import com.github.reload.net.data.FramedMessage;
import com.github.reload.net.data.FramedMessage.FramedData;
import com.github.reload.net.data.FramedMessageCodec;
import com.github.reload.net.data.HeadedMessageCodec;
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
		pipeline.addLast(FWD_DECODER, new HeadedMessageCodec(null));

		// IN: If message is directed to this node pass to upper layer,
		// otherwise forward
		// OUT: Send a forwarded message on the link (not originated locally)
		pipeline.addLast(FWD_HANDLER, fwdHandler);

		// IN: Codec for RELOAD message content and security block
		pipeline.addLast(MSG_DECODER, new MessageDecoder();

		// OUT: Codec for RELOAD message content and security block
		pipeline.addLast(MSG_ENCODER, new MessageEncoder(CodecFactory.getInstance(null)));

		// IN: Process incoming messages directed to this node
		pipeline.addLast(MSG_HANDLER, msgHandler);
	}

	public static void main(String[] args) throws Exception {
		ServerBootstrap sb = new ServerBootstrap();
		sb.group(new NioEventLoopGroup()).channel(NioServerSocketChannel.class).childHandler(new ChannelInitializerImpl(new LinkHandler() {

			@Override
			protected void handleReceived(FramedMessage message) {
				// TODO Auto-generated method stub

			}

			@Override
			protected FramedData getDataFrame(ByteBuf payload) {
				// TODO Auto-generated method stub
				return null;
			}
		}, new ForwardingHandler(), new MessageHandler())).childOption(ChannelOption.SO_KEEPALIVE, true);

		ChannelFuture f = sb.bind(8080);

		f.await();

		Bootstrap b = new Bootstrap();
		b.group(new NioEventLoopGroup()).channel(NioServerSocketChannel.class).handler(new ChannelInitializerImpl(new LinkHandler() {

			@Override
			protected void handleReceived(FramedMessage message) {
				// TODO Auto-generated method stub

			}

			@Override
			protected FramedData getDataFrame(ByteBuf payload) {
				// TODO Auto-generated method stub
				return null;
			}
		}, new ForwardingHandler(), new MessageHandler()));

		ChannelFuture f2 = b.connect(new InetSocketAddress(8080));

		f2.await();

		ChannelFuture f3 = f2.channel().write(new Message());
		f3.await();
		System.out.println(f.isSuccess());
	}
}
