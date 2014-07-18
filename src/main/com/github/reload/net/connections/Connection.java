package com.github.reload.net.connections;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPromise;
import com.github.reload.net.encoders.ForwardMessage;
import com.github.reload.net.encoders.ForwardMessageCodec;
import com.github.reload.net.encoders.Message;
import com.github.reload.net.encoders.header.NodeID;
import com.github.reload.net.stack.ReloadStack;

/**
 * A connection to a neighbor node
 */
public class Connection {

	private final NodeID nodeId;
	private final ReloadStack stack;

	public Connection(NodeID nodeId, ReloadStack stack) {
		this.nodeId = nodeId;
		this.stack = stack;
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
		ForwardMessageCodec fwdCodec = (ForwardMessageCodec) ch.pipeline().get(ReloadStack.HANDLER_FORWARD);

		ChannelPromise promise = ch.newPromise();

		ChannelHandlerContext context = ch.pipeline().context(ReloadStack.HANDLER_FORWARD);

		try {
			fwdCodec.write(context, headedMessage, promise);
		} catch (Exception e) {
			promise.setFailure(e);
		}

		return promise;
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

	@Override
	public String toString() {
		return "Connection to " + nodeId;
	}

}
