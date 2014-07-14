package com.github.reload.net.pipeline.handlers;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import com.github.reload.net.MessageRouter;
import com.github.reload.net.pipeline.encoders.HeadedMessage;

@Sharable
public class ForwardingHandlerTest extends ChannelDuplexHandler {

	public static final String NAME = "FWD_HANDLER";
	private ChannelHandlerContext ctx;
	private final MessageRouter router;

	public ForwardingHandlerTest(MessageRouter router) {
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
		ctx.fireChannelRead(message);
		// FIXME: If the peer is not responsible for it, forward the message
		// instead passing to upper layer
	}
}
