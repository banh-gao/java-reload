package com.github.reload;

import java.net.InetSocketAddress;
import com.github.reload.crypto.CryptoHelper;
import com.github.reload.net.encoders.content.errors.NetworkException;
import com.github.reload.net.encoders.header.NodeID;
import com.github.reload.net.encoders.secBlock.GenericCertificate.CertificateType;
import com.github.reload.routing.TopologyPlugin;

/**
 * Connector to be used for a specific overlay configuration, configure the
 * local
 * peer to operate with a specific overlay instance
 * 
 */
public abstract class ReloadConnector {

	private Configuration conf;

	private InetSocketAddress bootstrapAddr;
	private InetSocketAddress attachAddr;
	private boolean isOverlayInitiator;
	private boolean isClientMode = false;
	private NodeID localNodeId;

	public ReloadConnector(Configuration conf, ReloadConnector oldConnector) {
		this.conf = conf;
		if (oldConnector != null) {
			bootstrapAddr = connector.getBootstrapAddress();
			attachAddr = connector.getAttachAddress();
			isOverlayInitiator = connector.isOverlayInitiator;
		}
	}

	/**
	 * @return the data to be send in the join request
	 */
	protected abstract byte[] getJoinData();

	protected abstract TopologyPlugin getTopologyPlugin();

	protected abstract CertificateType getCertificateType();

	protected abstract CryptoHelper getCryptoHelper();

	protected Configuration getConfiguration() {
		return conf;
	}

	protected Configuration setConfiguration(Configuration newConf) {
		Configuration oldConf = conf;
		conf = newConf;
		return oldConf;
	}

	/**
	 * @return The address where the bootstrap server will be listening to
	 */
	public InetSocketAddress getBootstrapAddress() {
		return bootstrapAddr;
	}

	/**
	 * @return The address where the attach server will be listening to
	 */
	public InetSocketAddress getAttachAddress() {
		return attachAddr;
	}

	/**
	 * Set the address where the bootstrap server will be listening to. If the
	 * ip is a multicast address then a ping responder will be registered
	 * through IGMP on that multicast group.
	 * 
	 * @param attachAddr
	 */
	public void setBootstrapAddress(InetSocketAddress bootstrapAddr) {
		this.bootstrapAddr = bootstrapAddr;
	}

	/**
	 * Set the address where the attach server will be listening to
	 * 
	 * @param attachAddr
	 */
	public void setAttachAddress(InetSocketAddress attachAddr) {
		this.attachAddr = attachAddr;
	}

	/**
	 * Set if this peer is the initiator peer of the overlay (the first peer who
	 * doesn't have to join). If set to true this peer must also set a bootstrap
	 * server otherwise an initialization exception will be thrown on
	 * connecting.
	 * 
	 * @param isOverlayInitiator
	 * @see #setBootstrapAddress(InetSocketAddress)
	 */
	public void setOverlayInitiator(boolean isOverlayInitiator) {
		this.isOverlayInitiator = isOverlayInitiator;
	}

	/**
	 * @return True if this peer is the initiator peer of the overlay (the first
	 *         peer who doesn't have to join)
	 */
	public boolean isOverlayInitiator() {
		return isOverlayInitiator;
	}

	/**
	 * Set if this node must operate as a client rather than as a normal peer.
	 * In client mode the node will not collaborate to the overlay storage and
	 * message routing functionalities. Also the node is not directly reachable
	 * by its node-id, all the messages directed to this node must be forwarded
	 * through the connected neighbor.
	 * 
	 * @param isClientMode
	 */
	public void setClientMode(boolean isClientMode) {
		this.isClientMode = isClientMode;
	}

	/**
	 * Set the node-id of the local peer to be used in this instance. If not
	 * specified, it will be used the first one found in the peer certificate
	 * 
	 * @param localNodeId
	 */
	public void setLocalNodeId(NodeID localNodeId) {
		this.localNodeId = localNodeId;
	}

	/**
	 * Try to connect to the overlay by using the informations provided by this
	 * connector
	 * 
	 * @return The connection handler
	 * @throws InitializationException
	 *             if the initialization of the local peer fails
	 * @throws NetworkException
	 *             if some network error occurs
	 */
	public final ReloadOverlay connect() throws InitializationException, NetworkException {

		try {
			// TODO: enable when the instance raw xml extraction works
			// conf.verify(getCryptoHelper());
			ReloadOverlay conn = ReloadOverlay.getInstance(this);

			conn.connect();

			if (!isOverlayInitiator && !isClientMode())
				conn.join();

			return conn;
		} catch (NetworkException e) {
			throw e;
		} catch (Exception e) {
			throw new InitializationException(e);
		}
	}

	public boolean isBootstrapNode() {
		return bootstrapAddr != null;
	}

	public boolean isClientMode() {
		return isClientMode;
	}

	/**
	 * Used to compare overlay instances
	 */
	@Override
	public abstract boolean equals(Object obj);

	/**
	 * Used to map overlay instances
	 */
	@Override
	public abstract int hashCode();

	public NodeID getLocalNodeId() {
		return localNodeId;
	}
}
