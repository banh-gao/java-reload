package com.github.reload.routing;

import java.util.Set;
import com.github.reload.net.codecs.header.NodeID;
import com.github.reload.net.codecs.header.RoutableID;

/**
 * Routing table of the neighbor nodes managed by the topology plugin.
 * 
 */
public interface RoutingTable {

	/**
	 * @return the the next hops node-ids
	 */
	public Set<NodeID> getNextHops(RoutableID destination);

	public Set<NodeID> getNeighbors();
}
