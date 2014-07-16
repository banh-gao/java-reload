package com.github.reload.routing;

import java.util.Collection;
import java.util.List;
import com.github.reload.Configuration;
import com.github.reload.Context.Component;
import com.github.reload.InitializationException;
import com.github.reload.net.encoders.content.storage.StoreKindData;
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
public interface TopologyPlugin extends Component {

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
	 * Init the topology plugin with the given context
	 * 
	 * @param context
	 * 
	 * @throws InitializationException
	 */
	public void init(Configuration conf) throws InitializationException;

	/**
	 * Stop the topology plugin agent, this may be used to disconnect neighbors
	 * and deallocate system resources. This call must return only when the
	 * topology plugin has been completely stopped and deallocated.
	 */
	public void stop();

	/**
	 * @param destination
	 * @param ids
	 * @return The id in the collection closer to the given destination
	 */
	public <T extends RoutableID> T getCloserId(RoutableID destination, Collection<T> ids);

	/**
	 * Called when a new neighbor node has been attached to the local peer. Note
	 * that the new node can also be a bootstrap link, that neighbor must be
	 * associated will all the node-ids contained in the certificate excepts for
	 * those where there is already an associated entry in the routing table.
	 * 
	 * @param newNode
	 *            the new neighbor node
	 * @param updateRequested
	 *            if true, the remote peer has requested to receive a topology
	 *            plugin update message
	 */
	public void onNeighborConnected(NodeID newNode, boolean updateRequested);

	/**
	 * Called when a neighbor node is not anymore connected to the local peer.
	 * 
	 * @param node
	 *            the disconnected neighbor node
	 */
	public void onNeighborDisconnected(NodeID node);

	/**
	 * This method is called every time a message transmission to the specified
	 * neighbor node fails. The transmission can fail for different causes such
	 * as a network failure or, in case of an acked link, the acknowledgement
	 * message from the remote node was not received.
	 * 
	 * @param node
	 *            the remote node
	 */
	public void onTransmissionFailed(NodeID node);

	/**
	 * Called when new data are stored, the implementation may replicate the
	 * given data to other nodes that must be returned.
	 * The effective data replication should be executed in a separate thread
	 * since the returned node list needs to be reported back in the store
	 * answer.
	 * 
	 * @param data
	 *            the resourceId of the data
	 * @param data
	 *            the data to be replicated
	 * @return the id of the nodes where the data will be replicated
	 */
	public List<NodeID> onReplicateData(ResourceID resourceId, StoreKindData data);

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

	/**
	 * @return the path compression and decompression manager used to encode and
	 *         decode opaque destination ids
	 */
	public PathCompressor getPathCompressor();
}
