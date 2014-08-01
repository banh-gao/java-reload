package com.github.reload.net.stack;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.apache.log4j.Logger;
import com.github.reload.Bootstrap;
import com.github.reload.components.ComponentsContext;
import com.github.reload.net.MessageRouter;
import com.github.reload.net.encoders.ForwardMessage;
import com.github.reload.net.encoders.Header;
import com.github.reload.net.encoders.header.NodeID;
import com.github.reload.net.encoders.header.OpaqueID;
import com.github.reload.net.encoders.header.RoutableID;
import com.github.reload.routing.TopologyPlugin;

@Sharable
public class ForwardingHandler extends ChannelInboundHandlerAdapter {

	private final MessageRouter router;
	private final Bootstrap boot;
	private final TopologyPlugin plugin;

	public ForwardingHandler(ComponentsContext ctx) {
		this.router = ctx.get(MessageRouter.class);
		this.boot = ctx.get(Bootstrap.class);
		this.plugin = ctx.get(TopologyPlugin.class);
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		ForwardMessage message = (ForwardMessage) msg;

		RoutableID destination = message.getHeader().getDestinationId();

		while (destination instanceof OpaqueID) {
			decompressDestinationList(message.getHeader());
			destination = message.getHeader().getDestinationId();
		}

		if (isLocalResponsible(destination)) {
			Logger.getRootLogger().debug(String.format("Passing message %#x for local peer to upper layer...", message.getHeader().getTransactionId()));
			ctx.fireChannelRead(message);
		} else {
			Logger.getRootLogger().debug(String.format("Forwarding message %#x to neighbor...", message.getHeader().getTransactionId()));
			router.forwardMessage(message);
		}
	}

	private boolean isLocalResponsible(RoutableID destinationId) {
		NodeID localId = boot.getLocalNodeId();

		if (destinationId.isWildcard())
			return true;

		if (destinationId instanceof NodeID)
			return destinationId.equals(localId);

		return plugin.isLocalPeerResponsible(destinationId);
	}

	private void decompressDestinationList(Header header) {
		// FIXME: call path decompressor to decompress destination list from
		// opaque-id
	}
}
