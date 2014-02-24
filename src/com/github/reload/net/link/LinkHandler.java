package com.github.reload.net.link;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import com.github.reload.net.data.FramedMessage;
import com.github.reload.net.data.FramedMessage.FramedAck;
import com.github.reload.net.data.FramedMessage.FramedData;

/**
 * Subclasses will implement a specific link layer protocol to control the link
 */
public abstract class LinkHandler extends ChannelDuplexHandler {

	private ChannelHandlerContext ctx;

	@Override
	public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
		this.ctx = ctx;
	}

	@Override
	public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
		this.ctx = null;
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		FramedMessage frame = (FramedMessage) msg;
		handleReceived(frame);
		if (frame instanceof FramedData)
			ctx.fireChannelRead(((FramedData) frame).getData());
	}

	@Override
	public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
		ctx.write(getDataFrame((ByteBuf) msg));
	}

	/**
	 * Send acknowledgment message to the neighbor node
	 * 
	 * @param ack
	 * @return
	 */
	protected ChannelFuture sendAckFrame(FramedAck ack) {
		ChannelPromise promise = ctx.newPromise();
		try {
			write(ctx, ack, promise);
		} catch (Exception e) {
			promise.setFailure(e);
		}
		return promise;
	}

	protected abstract void handleReceived(FramedMessage message);

	/**
	 * Called when the upper layer want to send a message on the link
	 * 
	 * @param payload
	 * @return
	 */
	protected abstract FramedData getDataFrame(ByteBuf payload);
}
