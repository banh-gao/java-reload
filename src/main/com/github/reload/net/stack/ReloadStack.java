package com.github.reload.net.stack;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import java.net.SocketAddress;
import com.github.reload.net.encoders.Message;

public class ReloadStack {

	public static final String CODEC_FRAME = "CODEC_FRAME";
	public static final String DECODER_HEADER = "CODEC_HEADER";
	public static final String DECODER_PAYLOAD = "DECODER_PAYLOAD";
	public static final String ENCODER_MESSAGE = "ENCODER_MESSAGE";

	public static final String HANDLER_SSL = "HANDLER_SSL";
	public static final String HANDLER_LINK = "HANDLER_LINK";
	public static final String HANDLER_FORWARD = "HANDLER_FORWARD";
	public static final String HANDLER_DISPATCHER = "HANDLER_DISPATCHER";

	private final Channel channel;

	public ReloadStack(Channel channel) {
		this.channel = channel;
	}

	public ChannelFuture connect(SocketAddress remoteAddress) {
		return channel.connect(remoteAddress);
	}

	public ChannelFuture write(Message message) {
		return channel.writeAndFlush(message);
	}

	public void flush() {
		channel.flush();
	}

	public ChannelFuture disconnect() {
		return channel.disconnect();
	}

	public ChannelFuture shutdown() {
		return channel.close();
	}

	public Channel getChannel() {
		return channel;
	}
}