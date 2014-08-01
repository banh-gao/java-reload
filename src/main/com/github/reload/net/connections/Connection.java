package com.github.reload.net.connections;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.util.AttributeKey;
import com.github.reload.net.encoders.Codec;
import com.github.reload.net.encoders.ForwardMessage;
import com.github.reload.net.encoders.Header;
import com.github.reload.net.encoders.Message;
import com.github.reload.net.encoders.MessageEncoder;
import com.github.reload.net.encoders.header.NodeID;
import com.github.reload.net.stack.ReloadStack;

/**
 * A connection to a neighbor node
 */
public class Connection {

	public static final AttributeKey<Connection> CONNECTION = AttributeKey.valueOf("reloadConnection");

	private final Codec<Header> hdrCodec;
	private final NodeID nodeId;
	private final ReloadStack stack;

	public Connection(NodeID nodeId, ReloadStack stack) {
		hdrCodec = Codec.getCodec(Header.class, null);
		this.nodeId = nodeId;
		this.stack = stack;
		stack.getChannel().attr(CONNECTION).set(this);
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

		ByteBuf buf = ch.alloc().buffer();

		int messageStart = buf.writerIndex();

		try {
			hdrCodec.encode(headedMessage.getHeader(), buf);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		buf.writeBytes(headedMessage.getPayload());

		setMessageLength(buf, messageStart);

		// Get the context of the layer just before link handler, the write on
		// this context passes the given buffer to the link handler
		ChannelHandlerContext context = ch.pipeline().context(ReloadStack.DECODER_HEADER);

		return context.writeAndFlush(buf);
	}

	private void setMessageLength(ByteBuf buf, int messageStart) {
		int messageLength = buf.writerIndex() - messageStart;
		buf.setInt(messageStart + MessageEncoder.HDR_LEADING_LEN, messageLength);
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
