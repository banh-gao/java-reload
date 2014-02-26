package com.github.reload.net;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import com.github.reload.net.data.HeadedMessage;
import com.github.reload.routing.MessageRouter;

@Sharable
public class ForwardingHandler extends ChannelDuplexHandler {

	private ChannelHandlerContext ctx;
	private final MessageRouter router;

	public ForwardingHandler(MessageRouter router) {
		this.router = router;
	}

	@Override
	public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
		this.ctx = ctx;
	}

	@Override
	public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
		this.ctx = null;
	}

	/**
	 * Send message directly out of this channel
	 * 
	 * @param msg
	 * @return
	 */
	public ChannelPromise write(HeadedMessage msg) {
		if (ctx == null)
			throw new IllegalStateException("Handler not associated with a channel");

		ChannelPromise promise = ctx.newPromise();
		try {
			write(ctx, msg, promise);
		} catch (Exception e) {
			promise.setFailure(e);
		}
		return promise;
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		HeadedMessage message = (HeadedMessage) msg;
		if (router.isLocalMessage(message.getHeader())) {
			ctx.fireChannelRead(message);
		} else {
			router.sendMessage(message);
		}
	}
}
