package com.github.reload.net.stack;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import org.apache.log4j.Logger;
import com.github.reload.net.ForwardingRouter;
import com.github.reload.net.encoders.ForwardMessage;

@Sharable
public class ForwardingHandler extends ChannelDuplexHandler {

	private ForwardingRouter router;

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		ForwardMessage message = (ForwardMessage) msg;
		Logger.getRootLogger().debug(String.format("Passing message %#x for local peer to upper layer...", message.getHeader().getTransactionId()));
		ctx.fireChannelRead(message);
		// FIXME: If the peer is not responsible for it, forward the message
		// instead passing to upper layer
	}
}
