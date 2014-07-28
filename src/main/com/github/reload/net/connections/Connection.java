package com.github.reload.net.connections;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPromise;
import com.github.reload.net.encoders.Codec;
import com.github.reload.net.encoders.ForwardMessage;
import com.github.reload.net.encoders.Message;
import com.github.reload.net.encoders.header.Header;
import com.github.reload.net.encoders.header.NodeID;
import com.github.reload.net.stack.LinkHandler;
import com.github.reload.net.stack.ReloadStack;

/**
 * A connection to a neighbor node
 */
public class Connection {

	private final Codec<Header> hdrCodec;
	private final NodeID nodeId;
	private final ReloadStack stack;

	public Connection(NodeID nodeId, ReloadStack stack) {
		hdrCodec = Codec.getCodec(Header.class, null);
		this.nodeId = nodeId;
		this.stack = stack;
		stack.getChannel().attr(Message.PREVIOUS_HOP).set(nodeId);
	}

	/**
	 * Send the given message to the neighbor
	 * 
	 * @param message
	 * @return
	 */
	public ChannelFuture write(Message message) {
		return stack.write(message);
	}

	/**
	 * Forward the given message to the neighbor
	 * 
	 * @param headedMessage
	 * @return
	 */
	public ChannelFuture forward(ForwardMessage headedMessage) {
		Channel ch = stack.getChannel();
		LinkHandler lnkHandler = (LinkHandler) ch.pipeline().get(ReloadStack.HANDLER_LINK);

		ChannelPromise promise = ch.newPromise();

		ChannelHandlerContext context = ch.pipeline().context(ReloadStack.HANDLER_FORWARD);

		ByteBuf buf = ch.alloc().buffer();

		try {
			hdrCodec.encode(headedMessage.getHeader(), buf);
			buf.writeBytes(headedMessage.getPayload());
			lnkHandler.write(context, buf, promise);
		} catch (Exception e) {
			promise.setFailure(e);
		}

		return promise;
	}

	public ChannelFuture close() {
		return stack.shutdown();
	}

	public NodeID getNodeId() {
		return nodeId;
	}

	/**
	 * @return The Maximum Transmission Unit this link can carry measured in
	 *         bytes. If an outgoing message exceedes this limit, the message
	 *         will be automatically divided into smaller RELOAD fragments.
	 */
	protected int getLinkMTU() {
		return stack.getChannel().config().getOption(ChannelOption.SO_SNDBUF);
	}

	public ReloadStack getStack() {
		return stack;
	}

	@Override
	public String toString() {
		return "Connection to " + nodeId;
	}

}
