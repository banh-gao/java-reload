package com.github.reload.net.stack;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import java.net.SocketAddress;
import com.github.reload.net.encoders.Message;

public class ReloadStack {

	public static final String FRAME_CODEC = "FRM_CODEC";
	public static final String FWD_DECODER = "FWD_DECODER";
	public static final String MSG_DECODER = "MSG_DECODER";
	public static final String MSG_ENCODER = "MSG_ENCODER";

	public static final String CRYPTO_HANDLER = "CRYPTO_HANDLER";
	public static final String LINK_HANDLER = "LINK_HANDLER";
	public static final String FWD_HANDLER = "FWD_HANDLER";
	public static final String MSG_HANDLER = "MSG_HANDLER";

	private final Channel channel;

	public ReloadStack(Channel channel) {
		this.channel = channel;
	}

	public ChannelFuture connect(SocketAddress remoteAddress) {
		return channel.connect(remoteAddress);
	}

	public ChannelFuture write(Message message) {
		return channel.write(message);
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

	public ChannelHandler getHandler(String name) {
		return channel.pipeline().get(name);
	}
}