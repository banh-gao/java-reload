package com.github.reload;

import java.net.InetSocketAddress;
import java.util.Set;
import org.apache.log4j.Logger;
import com.github.reload.components.ComponentsContext;
import com.github.reload.conf.Configuration;
import com.github.reload.net.MessageRouter;
import com.github.reload.net.NetworkException;
import com.github.reload.net.connections.AttachConnector;
import com.github.reload.net.connections.Connection;
import com.github.reload.net.connections.ConnectionManager;
import com.github.reload.net.encoders.Message;
import com.github.reload.net.encoders.MessageBuilder;
import com.github.reload.net.encoders.content.JoinRequest;
import com.github.reload.net.encoders.header.DestinationList;
import com.github.reload.net.encoders.header.NodeID;
import com.github.reload.net.encoders.header.ResourceID;
import com.github.reload.net.ice.HostCandidate.OverlayLinkType;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

/**
 * This helper class is used to connect to the overlay and to send join requests
 */
class OverlayConnector {

	private static final Logger l = Logger.getRootLogger();

	private final ComponentsContext ctx;
	private final Overlay overlay;

	public OverlayConnector(ComponentsContext ctx) {
		this.ctx = ctx;
		overlay = new Overlay();
	}

	/**
	 * Start local components, connect to a bootstrap server and then to the
	 * admitting peer
	 * 
	 * @return the amount of local nodeids successful joined
	 * @throws InitializationException
	 * @throws NetworkException
	 */
	final SettableFuture<Overlay> connectToOverlay(final boolean joinNeeded) {

		final SettableFuture<Overlay> overlayConnFut = SettableFuture.create();

		Bootstrap bootstrap = ctx.get(Bootstrap.class);

		if (bootstrap.isOverlayInitiator()) {
			ctx.set(Overlay.class, overlay);
			overlayConnFut.set(overlay);
			return overlayConnFut;
		}

		Configuration conf = ctx.get(Configuration.class);

		ListenableFuture<Connection> bootConnFut = connectToBootstrap(conf.getBootstrapNodes(), conf.getOverlayLinkTypes());

		Futures.addCallback(bootConnFut, new FutureCallback<Connection>() {

			public void onSuccess(Connection neighbor) {
				attachToAP(neighbor.getNodeId(), overlayConnFut, joinNeeded);
			}

			@Override
			public void onFailure(Throwable t) {
				t.printStackTrace();
				// TODO Detect failure to connect to bootstrap nodes
			}
		});

		return overlayConnFut;
	}

	private ListenableFuture<Connection> connectToBootstrap(Set<InetSocketAddress> bootstrapNodes, Set<OverlayLinkType> linkTypes) {

		ConnectionManager connMgr = ctx.get(ConnectionManager.class);

		final SettableFuture<Connection> bootConnFut = SettableFuture.create();

		// Called by the first successful connection to a bootstrap node, other
		// successfully connection will be closed
		FutureCallback<Connection> connCB = new FutureCallback<Connection>() {

			@Override
			public void onSuccess(Connection result) {
				if (!bootConnFut.set(result))
					result.close();
			}

			@Override
			public void onFailure(Throwable t) {
				l.debug("Connection to bootstrap node failed", t);
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

	private void attachToAP(NodeID bootstrapServer, final SettableFuture<Overlay> overlayConnFut, final boolean joinNeeded) {

		Bootstrap bootstrap = ctx.get(Bootstrap.class);

		final DestinationList dest = new DestinationList(ResourceID.valueOf(bootstrap.getLocalNodeId().getData()));

		AttachConnector attachConnector = (AttachConnector) ctx.get(AttachConnector.class);

		ListenableFuture<Connection> apConnFut = attachConnector.attachTo(dest, bootstrapServer, true);

		Futures.addCallback(apConnFut, new FutureCallback<Connection>() {

			@Override
			public void onSuccess(Connection result) {
				l.info(String.format("Attach to admitting peer %s succeed", result.getNodeId()));

				if (joinNeeded)
					join(overlayConnFut);
				else {
					ctx.set(Overlay.class, overlay);
					overlayConnFut.set(overlay);
				}
			}

			@Override
			public void onFailure(Throwable t) {
				overlayConnFut.setException(t);
				l.info(String.format("Attach to admitting peer for %s failed", dest.getDestination()));
			}
		});

	}

	/**
	 * Join to the overlay through the admitting peer, return the
	 * join answer from the admitting peer
	 * 
	 * @return the join answer
	 */
	private final void join(final SettableFuture<Overlay> overlayConnFut) {
		l.info("Joining to RELOAD overlay in progress...");

		Bootstrap connector = ctx.get(Bootstrap.class);
		MessageBuilder msgBuilder = ctx.get(MessageBuilder.class);
		MessageRouter router = ctx.get(MessageRouter.class);

		JoinRequest req = new JoinRequest(connector.getLocalNodeId(), connector.getJoinData());

		DestinationList dest = new DestinationList(ResourceID.valueOf(connector.getLocalNodeId().getData()));

		Message request = msgBuilder.newMessage(req, dest);

		ListenableFuture<Message> joinAnsFut = router.sendRequestMessage(request);

		Futures.addCallback(joinAnsFut, new FutureCallback<Message>() {

			public void onSuccess(Message joinAns) {
				// TODO: check join answer with topology plugin
				l.info("Joining to RELOAD overlay completed.");
				ctx.set(Overlay.class, overlay);
				overlayConnFut.set(overlay);
			};

			@Override
			public void onFailure(Throwable t) {
				overlayConnFut.setException(t);
				l.info("Joining to RELOAD overlay failed.");
			}
		});
	}
}
