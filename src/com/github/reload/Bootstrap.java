package com.github.reload;

import java.net.InetSocketAddress;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.github.reload.conf.Configuration;
import com.github.reload.crypto.ReloadCertificate;
import com.github.reload.net.encoders.header.NodeID;
import com.github.reload.net.encoders.secBlock.HashAlgorithm;
import com.github.reload.net.encoders.secBlock.SignatureAlgorithm;
import com.google.common.util.concurrent.ListenableFuture;
import dagger.ObjectGraph;

/**
 * Connector used for a specific overlay configuration, it configures the
 * local peer to operate with a specific overlay instance
 * 
 */
public abstract class Bootstrap {

	public static HashAlgorithm DEFAULT_HASH = HashAlgorithm.SHA1;
	public static SignatureAlgorithm DEFAULT_SIGN = SignatureAlgorithm.RSA;

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

	public Configuration getConfiguration() {
		return conf;
	}

	protected List<Object> getProviderModules() {
		return Collections.emptyList();
	}

	/**
	 * @return the data to be send in the join request
	 */
	protected byte[] getJoinData() {
		return new byte[0];
	}

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

	public HashAlgorithm getSignHashAlg() {
		return DEFAULT_HASH;
	}

	public SignatureAlgorithm getSignAlg() {
		return DEFAULT_SIGN;
	}

	public HashAlgorithm getHashAlg() {
		return DEFAULT_HASH;
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
	 * Connects to the overlay
	 */
	public final ListenableFuture<Overlay> connect() {
		List<Object> modules = new ArrayList<Object>();
		CoreModule coreModule = new CoreModule();
		modules.add(coreModule);
		modules.addAll(getProviderModules());

		ObjectGraph g = ObjectGraph.create(modules.toArray());

		// Allow services to get a reference injection of the graph itself
		coreModule.graph = g;

		Overlay overlay = g.get(Overlay.class);

		overlay.init(this, coreModule);

		ListenableFuture<Overlay> overlayConnFut = overlay.connect();

		return overlayConnFut;
	}

	public boolean isClientMode() {
		return isClientMode;
	}

	public NodeID getLocalNodeId() {
		return localNodeId;
	}
}
