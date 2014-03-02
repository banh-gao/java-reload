package com.github.reload.routing;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import com.github.reload.message.NodeID;
import com.github.reload.message.RoutableID;
import com.github.reload.net.connections.Connection;

/**
 * Routing table of the neighbor nodes managed by the topology plugin.
 * 
 */
public interface RoutingTable extends Map<NodeID, Connection> {

	/**
	 * @return the the next hops node-ids
	 */
	public List<Connection> getNextHops(RoutableID destination);

	/**
	 * @return the the next hops node-ids, excluding the specified ones
	 */
	public List<Connection> getNextHops(RoutableID destination, Collection<? extends NodeID> excludedIds);
}
