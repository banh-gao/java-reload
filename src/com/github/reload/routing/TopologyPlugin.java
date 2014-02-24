package com.github.reload.routing;

import java.util.Collection;
import java.util.List;
import com.github.reload.Context;
import com.github.reload.message.Content;
import com.github.reload.message.HashAlgorithm;
import com.github.reload.message.NodeID;
import com.github.reload.message.ResourceID;
import com.github.reload.message.RoutableID;
import com.github.reload.message.content.JoinAnswer;
import com.github.reload.net.data.Message;

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
	 * Init the topology plugin with the given context
	 * 
	 * @param context
	 * 
	 * @throws InitializationException
	 */
	public void init(Context context) throws InitializationException;

	/**
	 * Called when the local peer has successfully joined into the overlay
	 * 
	 * @param answer
	 *            the join answer received from the admitting peer
	 */
	public void onJoinCompleted(Message answer);

	/**
	 * Stop the topology plugin agent, this may be used to disconnect neighbors
	 * and deallocate system resources. This call must return only when the
	 * topology plugin has been completely stopped and deallocated.
	 */
	public void stop();

	/**
	 * 
	 * @param storeRequest
	 *            the replica store request
	 * @return true if local node closer to the message destination is a valid
	 *         replica node for the given request
	 */
	public boolean isThisNodeValidReplicaFor(Message storeRequest);

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
	public void onNeighborConnected(NeighborNode newNode, boolean updateRequested);

	/**
	 * Called when a neighbor node is not anymore connected to the local peer.
	 * 
	 * @param node
	 *            the disconnected neighbor node
	 */
	public void onNeighborDisconnected(NeighborNode node);

	/**
	 * Called when another peer request this peer to join the overlay because
	 * this peer is the admitting peer for the joining peer.
	 * Returns the join answer to the joining node or throw an exception if the
	 * joining fails
	 * 
	 * @param joinRequest
	 *            The arrived join request
	 * @return the join answer that should be returned to the joining node in
	 *         case of successful join
	 * @throws Exception
	 *             if something fails in the join procedure, if the exception is
	 *             of type {@link ErrorMessageException} the error message will
	 *             be returned to the joining node
	 */
	public JoinAnswer onJoinRequest(Message joinRequest) throws Exception;

	/**
	 * Called when a neighbor node is leaving the overlay.
	 * This method is called only if the neighbor has send a leave message, it
	 * is not reliable to only use this method to manage routing table since a
	 * neighbor can disconnect from overlay without sending the leave message.
	 * 
	 * @param leaveRequest
	 *            the leave message from the leaving neighbor
	 * @return
	 * @throws Exception
	 *             if something fails, if the exception is of type
	 *             {@link ErrorMessageException} the error will be notified to
	 *             the sender
	 */
	public void onLeaveRequest(Message leaveRequest) throws Exception;

	/**
	 * This method is called every time a message transmission to the specified
	 * neighbor node fails. The transmission can fail for different causes such
	 * as a network failure or, in case of an acked link, the acknowledgement
	 * message from the remote node was not received.
	 * 
	 * @param node
	 *            the remote node
	 */
	public void onTransmissionFailed(NeighborNode node);

	/**
	 * Called when an update request is received from another node
	 * 
	 * @return the associated update answer
	 * 
	 * @throws Exception
	 *             if something fails, if the exception is of type
	 *             {@link ErrorMessageException} the error will be notified to
	 *             the sender
	 */
	public Message onUpdateRequest(Message updateRequest) throws Exception;

	/**
	 * Called when a route query request is received from another node
	 * 
	 * @return The associated route query answer
	 * 
	 * @throws Exception
	 *             if something fails, if the exception is of type
	 *             {@link ErrorMessageException} the error will be notified to
	 *             the sender
	 */
	public Content onRouteQueryRequest(Message routeQueryRequest) throws Exception;

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
	public List<NodeID> onReplicateData(ResourceID resourceId, StoredKindData data);

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
