package com.github.reload;

import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.Priority;
import com.github.reload.message.DestinationList;
import com.github.reload.message.NodeID;
import com.github.reload.message.ResourceID;
import com.github.reload.message.content.JoinRequest;
import com.github.reload.message.errors.NetworkException;
import com.github.reload.net.data.Message;
import com.github.reload.net.server.ReloadServer;
import com.google.common.util.concurrent.Service;

/**
 * This helper class is used by the {@link ReloadOverlay} class to manage main
 * overlay connection operations, it is responsible for overlay connecting,
 * leaving
 * and connection status detection
 */
class PeerInitializer {

	private static final int RECONNECT_ATTEMPTS = 1;

	private static final Logger logger = Logger.getLogger(ReloadOverlay.class);

	private final AtomicBoolean isConnected = new AtomicBoolean(true);

	private Context context;

	private NodeID admittingPeer;

	private Exception reconnectException;

	public PeerInitializer() {
		this.context = createContext();
	}

	private Context createContext() {
		Context context = new Context();
	}

	public void startServers() {
		context.attachServer = new ReloadServer(context, context.getConfiguration()null, null);
	}

	/**
	 * Start local servers, connect to a bootstrap server and then to the
	 * admitting peer
	 * 
	 * @return the amount of local nodeids successful joined
	 * @throws InitializationException
	 * @throws NetworkException
	 */
	final void startConnections() throws InitializationException, NetworkException {
		ReloadConnector connector = context.getConnector();
		if (connector.isOverlayInitiator() && !connector.isBootstrapNode())
			throw new InitializationException("The overlay initiator must be a bootstrap node");

		startAttachServer();
		startBootstrapServer();

		if (!connector.isOverlayInitiator()) {
			NeighborNode bootstrapServer = context.getBootstrapService().connectToBootstrapServer();

			try {
				admittingPeer = attachToAP(bootstrapServer);
				bootstrapServer.disconnect("Connected to admitting peer");

				context.getTopologyPlugin().onNeighborConnected(admittingPeer, false);
			} catch (ErrorMessageException e) {
				throw new NetworkException(e);
			} catch (InterruptedException e) {
				return;
			}
		}

		createLoopbackNode();

		isConnected.set(true);

		reconnectException = null;

		synchronized (isConnected) {
			isConnected.notifyAll();
		}

		context.getPeerStatusUpdater().setStartTime(System.currentTimeMillis());
	}

	private void createLoopbackNode() {
		LoopbackNode node = new LoopbackNode(context);
		context.setLoopbackNode(node);
		// FIXME: add loopback node to routing table
		// context.getTopologyPlugin().onNeighborConnected(node);
	}

	/**
	 * Join to the overlay through the admitting peer, return the
	 * join answer from the admitting peer
	 * 
	 * @return the join answer
	 * @throws NetworkException
	 * @throws InterruptedException
	 * @throws OverlayJoinException
	 */
	final void join() throws NetworkException, OverlayJoinException, InterruptedException {
		final ReloadConnector connector = context.getConnector();
		if (connector.isOverlayInitiator() || context.isClientMode()) {
			return;
		}

		try {
			JoinRequest req = new JoinRequest(context.getLocalId()) {

				@Override
				protected byte[] getData() {
					return connector.getJoinData();
				}
			};

			Message request = context.getMessageBuilder().newMessage(req, DestinationList.create(admittingPeer.getNodeId()));
			request.setPreferredNextHop(admittingPeer);

			Message answer;
			try {
				answer = context.getMessageRouter().sendRequestMessage(request);
			} catch (ErrorMessageException e) {
				throw new OverlayJoinException(e);
			} catch (NetworkException e) {
				throw new OverlayJoinException(e);
			}

			if (!answer.getSenderNeighbor().equals(admittingPeer))
				throw new OverlayJoinException("Join answer not coming from the admitting peer connection");

			context.getTopologyPlugin().onJoinCompleted(answer);
			logger.log(Level.DEBUG, context.getLocalId() + " has joined to " + context.getConfiguration().getOverlayName());
		} catch (Exception e) {
			logger.log(Level.WARN, e);
		}
	}

	private NeighborNode attachToAP(final NeighborNode bootstrapServer) throws NetworkException, InterruptedException, ErrorMessageException {
		DestinationList dest = DestinationList.create(ResourceID.valueOf(context.getLocalId().getData()));
		return context.getAttachManager().attachTo(dest, bootstrapServer, true);
	}

	private Service createAttachServer() throws InitializationException {
		ReloadConnector connector = context.getConnector();
		return new ReloadServer(context, connector.getAttachAddress(), linkType);
	}

	private void startBootstrapServer() throws InitializationException {
		ReloadConnector connector = context.getConnector();

		if (!connector.isBootstrapNode())
			return;

		try {
			new ReloadServer(connector.getBootstrapAddress());

		} catch (Exception e) {
			throw new InitializationException(e);
		}
	}

	/**
	 * Tries to reconnect the connection to the overlay up to
	 * {@value #RECONNECT_ATTEMPTS} times, if reconnect fails all subsequent
	 * calls to
	 * overlay operations will fail with a NetworkException
	 * 
	 * @param reason
	 *            The reason for the reconnecting that will be reported in logs,
	 *            may be null
	 * @return true if the reconnect succeeds, false otherwise
	 */
	public boolean reconnect(String reason) {
		reconnectException = null;
		if (reason != null && !reason.isEmpty())
			logger.log(Priority.INFO, "Reconnecting to overlay " + context.getConfiguration().getOverlayName() + ": " + reason);
		else
			logger.log(Priority.INFO, "Reconnecting to overlay " + context.getConfiguration().getOverlayName());

		leave();

		for (int i = 1; i <= RECONNECT_ATTEMPTS; i++) {
			try {
				startConnections();
				return true;
			} catch (Exception e) {
				reconnectException = e;
			}
		}
		return false;
	}

	/**
	 * Leave this overlay and release all the resources. This method returns
	 * when the overlay has been left.
	 */
	public void leave() {
		if (!isConnected())
			return;

		isConnected.set(false);

		AttachServerManager attachMgr = context.getAttachManager();
		attachMgr.requestTermination();

		BootstrapServerManager bootMgr = context.getBootstrapManager();
		bootMgr.requestTermination();

		try {
			attachMgr.join();
			bootMgr.join();
		} catch (InterruptedException e) {
			return;
		}

		context.getTopologyPlugin().stop();

		logger.log(Priority.INFO, "RELOAD peer " + context.getLocalId() + " disconnected from overlay " + context.getConfiguration().getOverlayName());
	}

	/**
	 * Locks the calling thread until the peer has been connected to the overlay
	 * 
	 * @throws NetworkException
	 *             if the reconnect attempts fails
	 */
	public void checkConnection() throws NetworkException {
		try {
			while (isConnected.get() == false) {
				if (reconnectException != null)
					throw new NetworkException(reconnectException);
				synchronized (isConnected) {
					isConnected.wait();
				}
			}
		} catch (InterruptedException e) {
			return;
		}
	}

	/**
	 * Change the connection connector to one compatible with the specified
	 * configuration, the new connector is initialized with the old connector
	 * parameters.
	 * 
	 * @param newConf
	 * @return true if the connector was changed successfully, false otherwise
	 */
	public boolean changeConnectorFor(Configuration newConf) {
		try {
			ReloadConnector newConnector = ReloadConnectorFactory.getInstance(newConf).createConnector(newConf, context.getConnector());
			context.init(newConnector);
			return true;
		} catch (Exception e) {
			reconnectException = e;
		}
		return false;
	}

	public boolean isConnected() {
		return isConnected.get();
	}
}
