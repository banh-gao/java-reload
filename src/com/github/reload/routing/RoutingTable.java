package com.github.reload.routing;

import java.util.Collection;
import java.util.List;
import java.util.SortedMap;
import com.github.reload.message.NodeID;
import com.github.reload.message.RoutableID;

/**
 * Routing table of the local node managed by the topology plugin.
 * 
 */
public interface RoutingTable {

	/**
	 * The neighbor node related the the given id
	 * 
	 * @param neighborId
	 * @return
	 */
	public NeighborNode getNeighbor(NodeID neighborId);

	/**
	 * @return all the neighbor nodes sorted by node id
	 */
	public SortedMap<NodeID, NeighborNode> getNeighbors();

	/**
	 * @return The size of the routing table
	 */
	public int getSize();

	/**
	 * @return the the next hops node-ids
	 */
	public List<NodeID> getNextHops(RoutableID destination);

	/**
	 * @return the the next hops node-ids, excluding the specified ones
	 */
	public List<NodeID> getNextHops(RoutableID destination, Collection<? extends NodeID> excludedIds);

	/**
	 * @param id
	 * @return true if the given id is in the routing table, false otherwise
	 */
	boolean contains(NodeID id);
}
