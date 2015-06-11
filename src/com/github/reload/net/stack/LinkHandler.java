package com.github.reload.net.stack;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.log4j.Logger;
import com.github.reload.components.ComponentsContext;
import com.github.reload.net.NetworkException;
import com.github.reload.net.connections.Connection;
import com.github.reload.net.connections.ConnectionManager;
import com.github.reload.net.connections.ConnectionManager.ConnectionStatusEvent.Type;
import com.github.reload.net.encoders.FramedMessage;
import com.github.reload.net.encoders.FramedMessage.FramedAck;
import com.github.reload.net.encoders.FramedMessage.FramedData;

/**
 * Subclasses will implement a specific link layer protocol to control the link
 */
public abstract class LinkHandler extends ChannelDuplexHandler {

	private final Logger l = Logger.getRootLogger();

	private ChannelHandlerContext ctx;

	private final Map<Long, Transmission> transmissions = new LinkedHashMap<Long, Transmission>();

	private final ComponentsContext compCtx;

	public LinkHandler(ComponentsContext compCtx) {
		this.compCtx = compCtx;
	}

	@Override
	public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
		this.ctx = ctx;
	}

	@Override
	public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
		this.ctx = null;
	}

	@Override
	public void close(ChannelHandlerContext ctx, ChannelPromise future) throws Exception {
		compCtx.postEvent(new ConnectionManager.ConnectionStatusEvent(Type.CLOSED, ctx.attr(Connection.CONNECTION).get()));
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		FramedMessage frame = (FramedMessage) msg;
		switch (frame.getType()) {
			case DATA :
				handleData((FramedData) frame);
				l.trace("Passing DATA frame " + frame.getSequence() + " to upper layer...");
				ctx.fireChannelRead(((FramedData) frame).getPayload());
				break;
			case ACK :
				l.trace("Received ACK for frame " + frame.getSequence());
				Transmission t = transmissions.remove(frame.getSequence());
				if (t != null) {
					handleAck((FramedAck) frame, t);
				} else {
					l.trace("Unexpected ACK message on " + ctx);
				}
			default :
				assert false;
				break;
		}
	}

	@Override
	public void write(final ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
		FramedData data = getDataFrame((ByteBuf) msg);
		Transmission t = new Transmission();
		t.promise = promise;
		transmissions.put(data.getSequence(), t);
		l.trace("Passing DATA frame " + data.getSequence() + " to lower layer...");
		ctx.write(data, promise);

		if (getLinkTimeout() > 0)
			t.startTimeout(getLinkTimeout());
	}

	/**
	 * Send acknowledgment message immediately
	 * 
	 * @param ack
	 * @return
	 */
	protected void sendAckFrame(FramedAck ack) {
		l.trace("Passing ACK frame " + ack.getSequence() + " to lower layer...");
		ctx.writeAndFlush(ack);
	}

	protected abstract void handleData(FramedData data);

	protected abstract void handleAck(FramedAck ack, Transmission request);

	/**
	 * Called when the upper layer want to send a message on the link
	 * 
	 * @param payload
	 * @return
	 */
	protected abstract FramedData getDataFrame(ByteBuf payload);

	protected long getLinkTimeout() {
		return 0;
	}

	class Transmission {

		public long startTime = System.currentTimeMillis();
		public ChannelPromise promise;

		public int getRTT() {
			return (int) (System.currentTimeMillis() - startTime);
		}

		public void startTimeout(long timeout) {
			ctx.executor().schedule(new Runnable() {

				@Override
				public void run() {
					promise.setFailure(new NetworkException("Unacked message from neighbor"));
				}
			}, timeout, TimeUnit.MILLISECONDS);
		}
	}
}
