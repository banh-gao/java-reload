package com.github.reload;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import org.apache.log4j.Logger;
import com.github.reload.components.ComponentsContext;
import com.github.reload.components.ComponentsContext.CompStart;
import com.github.reload.components.ComponentsContext.CompStop;
import com.github.reload.components.ComponentsRepository.Component;
import com.github.reload.components.MessageHandlersManager.MessageHandler;
import com.github.reload.conf.Configuration;
import com.github.reload.net.MessageRouter;
import com.github.reload.net.connections.Connection;
import com.github.reload.net.connections.ConnectionManager;
import com.github.reload.net.connections.ConnectionManager.ConnectionStatusEvent;
import com.github.reload.net.connections.ConnectionManager.ConnectionStatusEvent.Type;
import com.github.reload.net.encoders.Header;
import com.github.reload.net.encoders.Message;
import com.github.reload.net.encoders.MessageBuilder;
import com.github.reload.net.encoders.content.ContentType;
import com.github.reload.net.encoders.content.Error.ErrorType;
import com.github.reload.net.encoders.content.JoinAnswer;
import com.github.reload.net.encoders.content.JoinRequest;
import com.github.reload.net.encoders.content.LeaveRequest;
import com.github.reload.net.encoders.header.DestinationList;
import com.github.reload.net.encoders.header.NodeID;
import com.github.reload.net.encoders.header.ResourceID;
import com.github.reload.net.encoders.header.RoutableID;
import com.github.reload.net.ice.HostCandidate.OverlayLinkType;
import com.github.reload.routing.RoutingTable;
import com.github.reload.routing.TopologyPlugin;
import com.github.reload.services.storage.DataStorage;
import com.github.reload.services.storage.StoreKindData;
import com.github.reload.services.storage.encoders.StoreRequest;
import com.google.common.base.Optional;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

@Component(TopologyPlugin.class)
public class TestPlugin implements TopologyPlugin {

	private static final int RESID_LENGTH = 16;

	private static final Logger l = Logger.getRootLogger();

	@Component
	private ComponentsContext ctx;
	@Component
	private Configuration conf;
	@Component
	private Bootstrap boot;
	@Component
	private ConnectionManager connMgr;
	@Component
	private MessageRouter router;
	@Component
	private MessageBuilder msgBuilder;

	@Component
	private DataStorage store;

	private boolean isJoined = false;

	private final TestRouting r = new TestRouting();

	private NodeID TEST_REPLICA_NODE = NodeID.valueOf(new byte[16]);

	@CompStart
	private void start() {
		ctx.set(RoutingTable.class, r);
		if (boot.isOverlayInitiator())
			try {
				addLoopback().get();
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
			}
	}

	@Override
	public ListenableFuture<NodeID> requestJoin() {
		l.info(String.format("Joining to RELOAD overlay %s with %s in progress...", conf.getOverlayName(), boot.getLocalNodeId()));
		Bootstrap connector = ctx.get(Bootstrap.class);
		MessageBuilder msgBuilder = ctx.get(MessageBuilder.class);
		MessageRouter router = ctx.get(MessageRouter.class);

		JoinRequest req = new JoinRequest(connector.getLocalNodeId(), connector.getJoinData());

		DestinationList dest = new DestinationList(ResourceID.valueOf(connector.getLocalNodeId().getData()));

		Message request = msgBuilder.newMessage(req, dest);

		ListenableFuture<Message> joinAnsFut = router.sendRequestMessage(request);

		final SettableFuture<NodeID> joinFuture = SettableFuture.create();

		Futures.addCallback(joinAnsFut, new FutureCallback<Message>() {

			public void onSuccess(Message joinAns) {
				NodeID ap = joinAns.getHeader().getSenderId();
				addLoopback();
				r.neighbors.add(ap);
				l.info(String.format("Joining to RELOAD overlay %s with %s completed.", conf.getOverlayName(), boot.getLocalNodeId()));
				isJoined = true;
				joinFuture.set(joinAns.getHeader().getSenderId());
			};

			@Override
			public void onFailure(Throwable t) {
				l.info(String.format("Joining to RELOAD overlay %s with %s failed: %s", conf.getOverlayName(), boot.getLocalNodeId(), t.getMessage()));
				joinFuture.setException(t);
			}
		});

		return joinFuture;
	}

	private ListenableFuture<Connection> addLoopback() {
		ListenableFuture<Connection> fut = connMgr.connectTo(boot.getLocalAddress(), OverlayLinkType.TLS_TCP_FH_NO_ICE);
		Futures.addCallback(fut, new FutureCallback<Connection>() {

			@Override
			public void onSuccess(Connection result) {
				r.neighbors.add(result.getNodeId());
			}

			@Override
			public void onFailure(Throwable t) {
				t.printStackTrace();
			}
		});
		return fut;
	}

	@MessageHandler(ContentType.JOIN_REQ)
	public void handleJoinRequest(final Message req) {

		JoinAnswer ans = new JoinAnswer("JOIN ANS".getBytes());

		router.sendAnswer(req.getHeader(), ans);

		NodeID joinNode = ((JoinRequest) req.getContent()).getJoiningNode();

		if (connMgr.isNeighbor(joinNode))
			r.neighbors.add(joinNode);
	}

	@MessageHandler(ContentType.LEAVE_REQ)
	public void handleLeaveRequest(Message req) {
		Header head = req.getHeader();
		LeaveRequest leave = (LeaveRequest) req.getContent();
		NodeID leavingNode = leave.getLeavingNode();

		// Check sender id matches with the leaving node
		if (!head.getSenderId().equals(leavingNode)) {
			router.sendError(head, ErrorType.FORBITTEN, "Leaving node doesn't match with sender ID");
			return;
		}

		NodeID prevHop = head.getAttribute(Header.PREV_HOP);

		// Check neighbor id matches with the leaving node
		if (!prevHop.equals(leavingNode)) {
			router.sendError(head, ErrorType.FORBITTEN, "Leaving node is not a neighbor node");
			return;
		}

		r.neighbors.remove(leavingNode);

		l.debug(String.format("Node %s has left the overlay", leavingNode));
	}

	@Subscribe
	public void handleConnectionEvent(ConnectionStatusEvent e) {
		if (e.type == Type.ESTABLISHED) {
			r.neighbors.add(e.connection.getNodeId());
		}

		if (e.type == Type.CLOSED)
			r.neighbors.remove(e.connection.getNodeId());
	}

	@Override
	public int getResourceIdLength() {
		return RESID_LENGTH;
	}

	@Override
	public int getDistance(RoutableID source, RoutableID dest) {
		BigInteger f = new BigInteger(source.getData());
		BigInteger s = new BigInteger(dest.getData());
		return f.subtract(s).abs().mod(BigInteger.valueOf(Integer.MAX_VALUE)).intValue();
	}

	@Override
	public ResourceID getResourceId(byte[] resourceIdentifier) {
		return ResourceID.valueOf(Arrays.copyOfRange(resourceIdentifier, 0, RESID_LENGTH));
	}

	@Override
	public <T extends RoutableID> T getCloserId(RoutableID destination, Collection<T> ids) {
		if (destination == null)
			throw new NullPointerException();
		int dist = Integer.MAX_VALUE;
		T closer = null;
		for (T id : ids) {
			int tmp = getDistance(id, destination);
			if (tmp <= dist) {
				dist = tmp;
				closer = id;
			}
		}
		return closer;
	}

	@Override
	public boolean isLocalPeerResponsible(RoutableID destination) {
		int localDistance = Integer.MAX_VALUE;

		if (isJoined || boot.isOverlayInitiator()) {
			localDistance = getDistance(destination, boot.getLocalNodeId());
		}

		for (NodeID neighborId : r.neighbors) {
			int tmpDst = getDistance(neighborId, destination);
			if (tmpDst < localDistance)
				return false;
		}

		return true;
	}

	@Override
	public ListenableFuture<Void> requestLeave() {
		SettableFuture<Void> fut = SettableFuture.create();
		for (NodeID n : r.neighbors) {
			sendLeave(n);
		}

		fut.set(null);

		return fut;
	}

	private void sendLeave(final NodeID neighborNode) {
		DestinationList dest = new DestinationList();
		dest.add(neighborNode);
		dest.add(msgBuilder.getWildcard());

		Message leaveMessage = msgBuilder.newMessage(new LeaveRequest(boot.getLocalNodeId(), new byte[0]), dest);

		router.sendMessage(leaveMessage);
	}

	@CompStop
	private void stop() {
		sendLeaveAndClose();
	}

	private void sendLeaveAndClose() {

	}

	private class TestRouting implements RoutingTable {

		private final SortedSet<NodeID> neighbors = new TreeSet<NodeID>();

		@Override
		public Set<NodeID> getNextHops(RoutableID destination) {

			Set<NodeID> candidates = new HashSet<NodeID>(neighbors);

			if (candidates.isEmpty())
				return Collections.emptySet();

			// Remove loopback connection from results if destination is not
			// itself and there are other available neighbors
			if (!destination.equals(boot.getLocalNodeId()) && candidates.size() > 1)
				candidates.remove(boot.getLocalNodeId());

			NodeID singleNextHop = getCloserId(destination, candidates);

			return Collections.singleton(singleNextHop);
		}

		@Override
		public Set<NodeID> getNeighbors() {
			return Collections.unmodifiableSet(neighbors);
		}
	}

	@Override
	public ListenableFuture<NodeID> requestUpdate(NodeID neighborNode) {
		// NO update
		return null;
	}

	@Override
	public boolean isLocalPeerValidStorage(ResourceID resourceId, boolean isReplica) {
		return true;
	}

	@Override
	public List<NodeID> getReplicaNodes(ResourceID resourceId) {
		return Collections.singletonList(TEST_REPLICA_NODE);
	}

	@Override
	public void requestReplication(ResourceID resourceId) {
		List<NodeID> replicaNodes = getReplicaNodes(resourceId);

		Optional<Map<Long, StoreKindData>> res = store.get(resourceId);

		if (!res.isPresent())
			return;

		Collection<StoreKindData> data = res.get().values();

		short replNum = 1;
		for (NodeID repl : replicaNodes) {
			replicateData(msgBuilder.newMessage(new StoreRequest(resourceId, replNum, data), new DestinationList(repl)));
			replNum++;
		}
	}

	private void replicateData(Message replicaStore) {
		router.sendRequestMessage(replicaStore);
	}
}
