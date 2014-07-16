package com.github.reload.net;

import com.github.reload.net.connections.Connection;
import com.github.reload.net.connections.ConnectionManager;
import com.github.reload.net.encoders.HeadedMessage;
import com.github.reload.net.encoders.header.NodeID;
import com.github.reload.routing.RoutingTable;
import com.github.reload.routing.TopologyPlugin;

/**
 * Forwards only messages not addressed to this peer. Forwards them without
 * decoding the payload
 */
public class ForwardingRouter {

	private final RoutingTable routingTable;
	private final ConnectionManager connMgr;

	public ForwardingRouter(TopologyPlugin plugin, ConnectionManager connMgr) {
		this.routingTable = plugin.getRoutingTable();
		this.connMgr = connMgr;
	}

	public void forwardMessage(HeadedMessage msg) {
		for (NodeID nextHop : routingTable.getNextHops(msg.getHeader().getDestinationId())) {
			Connection c = connMgr.getConnection(nextHop);
			// Forward message and ignore delivery status
			c.forward(msg);
		}
	}

}
