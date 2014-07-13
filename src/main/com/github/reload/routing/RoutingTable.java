package com.github.reload.routing;

import java.util.Collection;
import java.util.Set;
import com.github.reload.message.NodeID;
import com.github.reload.message.RoutableID;

/**
 * Routing table of the neighbor nodes managed by the topology plugin.
 * 
 */
public interface RoutingTable extends Set<NodeID> {

	/**
	 * @return the the next hops node-ids
	 */
	public Set<NodeID> getNextHops(RoutableID destination);

	/**
	 * @return the the next hops node-ids, excluding the specified ones
	 */
	public Set<NodeID> getNextHops(RoutableID destination, Collection<? extends NodeID> excludedIds);
}
