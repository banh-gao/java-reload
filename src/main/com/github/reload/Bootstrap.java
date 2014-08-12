package com.github.reload;

import java.net.InetSocketAddress;
import java.security.PrivateKey;
import com.github.reload.components.ComponentsContext;
import com.github.reload.components.ComponentsRepository;
import com.github.reload.conf.Configuration;
import com.github.reload.crypto.ReloadCertificate;
import com.github.reload.net.AttachService;
import com.github.reload.net.MessageRouter;
import com.github.reload.net.connections.ConnectionManager;
import com.github.reload.net.encoders.MessageBuilder;
import com.github.reload.net.encoders.header.NodeID;
import com.github.reload.net.encoders.secBlock.GenericCertificate.CertificateType;
import com.github.reload.net.ice.ICEHelper;
import com.github.reload.services.PingService;
import com.github.reload.services.storage.StorageService;
import com.google.common.util.concurrent.ListenableFuture;

/**
 * Connector used for a specific overlay configuration, it configures the
 * local peer to operate with a specific overlay instance
 * 
 */
public abstract class Bootstrap {

	private final Configuration conf;
	private InetSocketAddress localAddr;
	private boolean isOverlayInitiator;
	private boolean isClientMode = false;
	private NodeID localNodeId;

	private ReloadCertificate localCert;
	private PrivateKey localKey;

	public Bootstrap(Configuration conf) {
		this.conf = conf;
	}

	/**
	 * @return the data to be send in the join request
	 */
	protected abstract byte[] getJoinData();

	protected abstract CertificateType getCertificateType();

	public void setLocalCert(ReloadCertificate localCert) {
		this.localCert = localCert;
	}

	public void setLocalKey(PrivateKey localKey) {
		this.localKey = localKey;
	}

	public ReloadCertificate getLocalCert() {
		return localCert;
	}

	public PrivateKey getLocalKey() {
		return localKey;
	}

	/**
	 * @return The address where the server will be listening to
	 */
	public InetSocketAddress getLocalAddress() {
		return localAddr;
	}

	/**
	 * Set the address where the server will be listening to.
	 * 
	 * @param attachAddr
	 */
	public void setLocalAddress(InetSocketAddress localAddr) {
		this.localAddr = localAddr;
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
	 *         if the initialization of the local peer fails
	 */
	public final ListenableFuture<Overlay> connect() {
		registerComponents();

		ComponentsRepository.register(MessageBuilder.class);
		ComponentsRepository.register(ConnectionManager.class);
		ComponentsRepository.register(AttachService.class);
		ComponentsRepository.register(ICEHelper.class);
		ComponentsRepository.register(MessageRouter.class);

		ComponentsContext ctx = ComponentsContext.newInstance();
		ctx.set(Configuration.class, conf);
		ctx.set(Bootstrap.class, this);

		// setup default components
		ctx.set(ConnectionManager.class, new ConnectionManager());
		ctx.set(StorageService.class, new StorageService());
		ctx.set(AttachService.class, new AttachService());
		ctx.set(PingService.class, new PingService());

		ctx.startComponents();

		ListenableFuture<Overlay> overlayConnFut = new OverlayConnector(ctx).connectToOverlay(!isClientMode);

		return overlayConnFut;
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

	protected void registerComponents() {

	}
}
