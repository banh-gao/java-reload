package com.github.reload;

import java.net.InetSocketAddress;
import java.util.Set;
import org.apache.log4j.Logger;
import com.github.reload.conf.Configuration;
import com.github.reload.net.AttachService;
import com.github.reload.net.NetworkException;
import com.github.reload.net.connections.Connection;
import com.github.reload.net.connections.ConnectionManager;
import com.github.reload.net.encoders.header.DestinationList;
import com.github.reload.net.encoders.header.NodeID;
import com.github.reload.net.encoders.header.ResourceID;
import com.github.reload.net.ice.HostCandidate.OverlayLinkType;
import com.github.reload.routing.TopologyPlugin;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

/**
 * This helper class is used to connect and join to the overlay
 */
class OverlayConnector {

	/**
	 * 
	 */
	private final Overlay overlay;
	private final TopologyPlugin topology;
	private final ConnectionManager connMgr;

	public OverlayConnector(Overlay overlay, TopologyPlugin topology, ConnectionManager connMgr) {
		this.overlay = overlay;
		this.topology = topology;
		this.connMgr = connMgr;
	}

	private final Logger l = Logger.getRootLogger();

	/**
	 * Connect to a bootstrap server and then to the admitting peer
	 * 
	 * @return the amount of local nodeids successful joined
	 */
	final SettableFuture<Overlay> connectToOverlay(Configuration conf) {

		final SettableFuture<Overlay> overlayConnFut = SettableFuture.create();

		if (this.overlay.isOverlayInitiator()) {
			l.info(String.format("RELOAD overlay %s initialized by %s at %s.", conf.get(Configuration.OVERLAY_NAME), overlay.getLocalNodeId(), overlay.getLocalAddress()));
			overlayConnFut.set(this.overlay);
			return overlayConnFut;
		}

		ListenableFuture<Connection> bootConnFut = connectToBootstrap(conf.get(Configuration.BOOT_NODES), conf.get(Configuration.LINK_TYPES));

		Futures.addCallback(bootConnFut, new FutureCallback<Connection>() {

			@Override
			public void onSuccess(Connection neighbor) {
				attachToAP(neighbor.getNodeId(), overlayConnFut);
			}

			@Override
			public void onFailure(Throwable t) {
				overlayConnFut.setException(t);
			}
		});

		return overlayConnFut;
	}

	private ListenableFuture<Connection> connectToBootstrap(final Set<InetSocketAddress> bootstrapNodes, Set<OverlayLinkType> linkTypes) {

		final SettableFuture<Connection> bootConnFut = SettableFuture.create();

		// Called by the first successful connection to a bootstrap node,
		// other
		// successfully connection will be closed
		FutureCallback<Connection> connCB = new FutureCallback<Connection>() {

			int remainingServers = bootstrapNodes.size();

			@Override
			public void onSuccess(Connection result) {
				if (!bootConnFut.set(result)) {
					result.close();
				}
			}

			@Override
			public void onFailure(Throwable t) {
				if (remainingServers == 0) {
					bootConnFut.setException(new NetworkException("Cannot connect to any bootstrap server"));
				} else {
					remainingServers--;
				}
			}
		};

		for (InetSocketAddress node : bootstrapNodes) {
			for (OverlayLinkType linkType : linkTypes) {
				ListenableFuture<Connection> conn = connMgr.connectTo(node, linkType);
				Futures.addCallback(conn, connCB);
			}
		}

		return bootConnFut;
	}

	private void attachToAP(NodeID bootstrapServer, final SettableFuture<Overlay> overlayConnFut) {
		final DestinationList dest = new DestinationList();

		// Pass request through bootstrap server
		dest.add(bootstrapServer);

		// ResourceId destination corresponding to local node-id to route
		// the
		// attach request to the correct Admitting Peer
		dest.add(ResourceID.valueOf(overlay.getLocalNodeId().getData()));

		AttachService attachConnector = overlay.getService(AttachService.class);

		ListenableFuture<Connection> apConnFut = attachConnector.attachTo(dest, true);

		Futures.addCallback(apConnFut, new FutureCallback<Connection>() {

			@Override
			public void onSuccess(Connection apConn) {
				if (!overlay.isClientMode()) {
					ListenableFuture<NodeID> joinCB = topology.requestJoin();
					Futures.addCallback(joinCB, new FutureCallback<NodeID>() {

						@Override
						public void onSuccess(NodeID result) {
							overlayConnFut.set(overlay);
						}

						@Override
						public void onFailure(Throwable t) {
							overlayConnFut.setException(t);
						}
					});
				} else {
					overlayConnFut.set(overlay);
				}
			}

			@Override
			public void onFailure(Throwable t) {
				overlayConnFut.setException(t);
			}
		});

	}
}