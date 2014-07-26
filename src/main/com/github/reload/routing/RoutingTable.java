package com.github.reload.routing;

import java.util.Collection;
import java.util.Set;
import com.github.reload.Components.Component;
import com.github.reload.net.encoders.header.NodeID;
import com.github.reload.net.encoders.header.RoutableID;

/**
 * Routing table of the neighbor nodes managed by the topology plugin.
 * 
 */
@Component(RoutingTable.COMPNAME)
public interface RoutingTable {

	public static final String COMPNAME = "com.github.reload.routing.RoutingTable";

	/**
	 * @return the the next hops node-ids
	 */
	public Set<NodeID> getNextHops(RoutableID destination);

	/**
	 * @return the the next hops node-ids, excluding the specified ones
	 */
	public Set<NodeID> getNextHops(RoutableID destination, Collection<? extends NodeID> excludedIds);
}
