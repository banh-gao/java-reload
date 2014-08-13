package com.github.reload.net.stack;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.apache.log4j.Logger;
import com.github.reload.Bootstrap;
import com.github.reload.components.ComponentsContext;
import com.github.reload.net.MessageRouter;
import com.github.reload.net.connections.ConnectionManager;
import com.github.reload.net.encoders.ForwardMessage;
import com.github.reload.net.encoders.header.DestinationList;
import com.github.reload.net.encoders.header.NodeID;
import com.github.reload.net.encoders.header.RoutableID;
import com.github.reload.routing.TopologyPlugin;

@Sharable
public class ForwardingHandler extends ChannelInboundHandlerAdapter {

	private static final Logger l = Logger.getRootLogger();

	private final MessageRouter router;
	private final Bootstrap boot;
	private final TopologyPlugin plugin;
	private final ConnectionManager connMgr;

	public ForwardingHandler(ComponentsContext ctx) {
		router = ctx.get(MessageRouter.class);
		boot = ctx.get(Bootstrap.class);
		plugin = ctx.get(TopologyPlugin.class);
		connMgr = ctx.get(ConnectionManager.class);
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		ForwardMessage message = (ForwardMessage) msg;

		// FIXME: drop if ttl exceeds

		DestinationList destList = message.getHeader().getDestinationList();

		NodeID localId = boot.getLocalNodeId();

		processDestination(destList, localId);

		if (!isValidDestination(destList, localId)) {
			l.debug(String.format("Invalid message %#x destination dropped...", message.getHeader().getTransactionId()));
			return;
		}

		RoutableID nextHop = destList.get(0);

		if (isLocalPeerResponsible(nextHop)) {
			l.debug(String.format("Passing message %#x for local peer to upper layer...", message.getHeader().getTransactionId()));
			ctx.fireChannelRead(message);
		} else {
			l.debug(String.format("Forwarding message %#x to neighbor...", message.getHeader().getTransactionId()));
			router.forwardMessage(message);
		}
	}

	private void processDestination(DestinationList destList, NodeID localId) {
		RoutableID nextHop = destList.get(0);

		switch (nextHop.getType()) {
			case OPAQUEID :
				decompressDestinationList(destList);
				processDestination(destList, localId);

			case NODEID :
				if (nextHop.equals(localId) && destList.size() > 1) {
					destList.remove(0);
					processDestination(destList, localId);
				}

			case RESOURCEID :
				return;
		}

		throw new IllegalStateException();
	}

	private boolean isValidDestination(DestinationList destList, NodeID localId) {
		RoutableID nextHop = destList.get(0);

		switch (nextHop.getType()) {
			case OPAQUEID :
				return true;

			case RESOURCEID :
				return destList.size() == 1;

			case NODEID :
				if (nextHop.equals(localId))
					return true;

				return connMgr.isNeighbor((NodeID) nextHop);
		}

		throw new IllegalStateException();
	}

	private void decompressDestinationList(DestinationList destList) {
		// FIXME: call path decompressor to decompress destination list from
		// opaque-id
	}

	private boolean isLocalPeerResponsible(RoutableID dest) {

		if (dest.isWildcard())
			return true;

		if (dest.equals(boot.getLocalNodeId()))
			return true;

		return plugin.isLocalPeerResponsible(dest);
	}
}
