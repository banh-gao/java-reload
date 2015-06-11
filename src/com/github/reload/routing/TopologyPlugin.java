package com.github.reload.routing;

import java.util.Collection;
import java.util.List;
import com.github.reload.net.encoders.header.NodeID;
import com.github.reload.net.encoders.header.ResourceID;
import com.github.reload.net.encoders.header.RoutableID;
import com.google.common.util.concurrent.ListenableFuture;

/**
 * The algorithm that performs the resource based routing and controls the
 * overlay, each instance of this class is responsible for a single nodeid of
 * the local peer
 * 
 */
public interface TopologyPlugin {

	/**
	 * Requests to join into the overlay through the given admitting peer
	 * 
	 * @return the node-id of the peer who answers to the join
	 */
	public ListenableFuture<NodeID> requestJoin();

	/**
	 * Requests to send an update message to the given neighbor
	 */
	public ListenableFuture<NodeID> requestUpdate(NodeID neighborNode);

	/**
	 * Requests to leave message to the given neighbor
	 */
	public ListenableFuture<Void> requestLeave();

	/**
	 * @return the length in bytes of resource identifiers used by this plugin
	 */
	public int getResourceIdLength();

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

	public int getDistance(RoutableID source, RoutableID dest);

	public boolean isLocalPeerResponsible(RoutableID dest);

	public boolean isLocalPeerValidStorage(ResourceID resourceId, boolean isReplica);

	public List<NodeID> getReplicaNodes(ResourceID resourceId);

	public void requestReplication(ResourceID resourceId);
}