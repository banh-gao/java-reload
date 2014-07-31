package com.github.reload.routing;

import java.util.Collection;
import com.github.reload.net.encoders.header.NodeID;
import com.github.reload.net.encoders.header.ResourceID;
import com.github.reload.net.encoders.header.RoutableID;
import com.github.reload.net.encoders.secBlock.HashAlgorithm;

/**
 * The algorithm that performs the resource based routing and controls the
 * overlay, each instance of this class is responsible for a single nodeid of
 * the local peer
 * 
 */
public interface TopologyPlugin {

	/**
	 * @return the length in bytes of resource identifiers used by this plugin
	 */
	public int getResourceIdLength();

	/**
	 * @return the hash algorithm used in the overlay
	 */
	public HashAlgorithm getHashAlgorithm();

	/**
	 * Compute the resource id, the resulting id length must be equals to the
	 * value returned by the {@link TopologyPlugin#getResourceIdLength()} method
	 * 
	 * @param resourceIdentifier
	 *            the resource identifier from the user
	 * @return the resource id used by the topology algorithm in the overlay
	 */
	public ResourceID getResourceId(byte[] resourceIdentifier);

	/**
	 * @param destination
	 * @param ids
	 * @return The id in the collection closer to the given destination
	 */
	public <T extends RoutableID> T getCloserId(RoutableID destination, Collection<T> ids);

	/**
	 * 
	 * @return The routing table associated to the given local peer node-id
	 */
	public RoutingTable getRoutingTable();

	/**
	 * @return true if the local peer is the responsible for the given
	 *         destination
	 */
	public boolean isThisPeerResponsible(RoutableID destinationId);

	/**
	 * @return true if the local peer is the responsible for the given
	 *         destination, the specified neighbors are excluded from the
	 *         decision
	 */
	public boolean isThisPeerResponsible(RoutableID destinationId, Collection<? extends NodeID> excludedNeighbors);

	int getDistance(RoutableID source, RoutableID dest);
}
