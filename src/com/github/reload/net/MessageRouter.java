package com.github.reload.net;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.github.reload.message.Header;
import com.github.reload.message.NodeID;
import com.github.reload.net.connections.Connection;
import com.github.reload.net.data.Message;
import com.github.reload.routing.RoutingTable;
import com.google.common.util.concurrent.AbstractFuture;

/**
 * Route the outgoing messages to neighbor nodes by using the routing table
 */
public class MessageRouter {

	private final RoutingTable routingTable;

	public MessageRouter(RoutingTable routingTable) {
		this.routingTable = routingTable;
	}

	public ForwardFuture sendMessage(Message message) {
		Header header = message.getHeader();

		List<Connection> hops = routingTable.getNextHops(header.getDestinationId());

		ForwardFuture fwdFut = new ForwardFuture(routingTable.keySet());

		for (Connection nextHop : hops) {
			forward(message, nextHop, fwdFut);
		}

		return fwdFut;
	}

	private void forward(Message message, Connection conn, ForwardFuture fwdFuture) {
		ChannelFuture chFut = conn.write(message);
		fwdFuture.addChannelFuture(conn.getNodeId(), chFut);
	}

	/**
	 * This object controls the transmission status of an outgoing
	 * message to the neighbor nodes
	 */
	public static class ForwardFuture extends AbstractFuture<Boolean> {

		private final Set<NodeID> pendingNeighbors;
		private final Map<NodeID, Throwable> failedNeighbors;
		private final Set<NodeID> successNeighbors;

		public ForwardFuture(Set<NodeID> neighbors) {
			this.pendingNeighbors = neighbors;
			successNeighbors = new HashSet<NodeID>();
			failedNeighbors = new HashMap<NodeID, Throwable>();

		}

		public boolean isDone() {
			return pendingNeighbors.size() == 0;
		}

		public boolean isSuccess() {
			return isDone() && failedNeighbors.size() == 0;
		}

		public Map<NodeID, Throwable> getFailedNeighbors() {
			return failedNeighbors;
		}

		public Set<NodeID> getPendingNeighbors() {
			return pendingNeighbors;
		}

		public Set<NodeID> getSuccessNeighbors() {
			return successNeighbors;
		}

		void addChannelFuture(final NodeID neighbor, ChannelFuture chFut) {
			chFut.addListener(new ChannelFutureListener() {

				@Override
				public void operationComplete(ChannelFuture future) throws Exception {
					synchronized (future) {
						if (future.isSuccess())
							successNeighbors.add(neighbor);
						else
							failedNeighbors.put(neighbor, future.cause());

						pendingNeighbors.remove(neighbor);

						if (pendingNeighbors.size() == 0) {
							if (isSuccess())
								set(isSuccess());
							else
								setException(new ForwardingException(failedNeighbors));
						}
					}
				}
			});
		}
	}

	public static class ForwardingException extends Exception {

		private final Map<NodeID, Throwable> failures;

		public ForwardingException(Map<NodeID, Throwable> failures) {
			this.failures = failures;
		}

		public Map<NodeID, Throwable> getFailures() {
			return failures;
		}

	}
}
