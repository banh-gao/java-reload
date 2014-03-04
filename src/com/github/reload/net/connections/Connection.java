package com.github.reload.net.connections;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import com.github.reload.message.NodeID;
import com.github.reload.net.data.HeadedMessage;
import com.github.reload.net.ice.IceCandidate.OverlayLinkType;

/**
 * A connection to a neighbor node
 */
public class Connection {

	private final NodeID nodeId;
	private final OverlayLinkType linkType;
	private final Channel channel;

	public Connection(OverlayLinkType linkType, Channel channel, NodeID nodeId) {
		this.linkType = linkType;
		this.channel = channel;
		this.nodeId = nodeId;
	}

	public ChannelFuture write(HeadedMessage message) {
		return channel.write(message);
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
		return channel.config().getOption(ChannelOption.SO_SNDBUF);
	}

	public OverlayLinkType getLinkType() {
		return linkType;
	}

	@Override
	public String toString() {
		return "Connection to " + nodeId;
	}

}
