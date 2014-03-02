package com.github.reload.net.connections;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import java.util.Collections;
import org.apache.log4j.Logger;
import com.github.reload.message.NodeID;
import com.github.reload.net.MessageRouter.ForwardPromise;
import com.github.reload.net.data.Message;
import com.github.reload.net.ice.IceCandidate.OverlayLinkType;
import com.google.common.util.concurrent.ListenableFuture;

/**
 * A connection to a neighbor node
 */
public class Connection {

	private final Logger l = Logger.getRootLogger();
	private final NodeID nodeId;
	private final OverlayLinkType linkType;
	private final boolean isBootstrapLink;
	private final Channel channel;

	public Connection(OverlayLinkType linkType, Channel channel) {
		this.linkType = linkType;
		this.channel = channel;
	}

	public ChannelFuture write(Message message) {
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
		return TX_BUFFER_SIZE;
	}

	/**
	 * @return True if the neighbor connection was established only for
	 *         bootstrap
	 */
	public boolean isBootstrapLink() {
		return isBootstrapLink;
	}

	public OverlayLinkType getLinkType() {
		return linkType;
	}

	public ListenableFuture<NodeID> disconnect(String reason) {

		// TODO: send leave message and close connection

		ForwardPromise fut = new ForwardPromise(Collections.singleton(nodeId));

		ChannelFuture f = channel.close();

	}

	@Override
	public String toString() {
		return "Connection to " + nodeId;
	}

}
