package com.github.reload;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import org.apache.log4j.Logger;
import com.github.reload.components.ComponentsContext;
import com.github.reload.components.ComponentsContext.CompStart;
import com.github.reload.components.ComponentsContext.CompStop;
import com.github.reload.components.ComponentsRepository.Component;
import com.github.reload.components.MessageHandlersManager.MessageHandler;
import com.github.reload.conf.Configuration;
import com.github.reload.net.MessageRouter;
import com.github.reload.net.connections.ConnectionManager;
import com.github.reload.net.connections.ConnectionManager.ConnectionStatusEvent;
import com.github.reload.net.connections.ConnectionManager.ConnectionStatusEvent.Type;
import com.github.reload.net.encoders.Message;
import com.github.reload.net.encoders.MessageBuilder;
import com.github.reload.net.encoders.content.ContentType;
import com.github.reload.net.encoders.content.Error;
import com.github.reload.net.encoders.content.Error.ErrorType;
import com.github.reload.net.encoders.content.JoinAnswer;
import com.github.reload.net.encoders.content.JoinRequest;
import com.github.reload.net.encoders.content.LeaveRequest;
import com.github.reload.net.encoders.header.DestinationList;
import com.github.reload.net.encoders.header.NodeID;
import com.github.reload.net.encoders.header.ResourceID;
import com.github.reload.net.encoders.header.RoutableID;
import com.github.reload.routing.RoutingTable;
import com.github.reload.routing.TopologyPlugin;
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

	private boolean isJoined = false;

	private final TestRouting r = new TestRouting();

	@CompStart
	private void start() {
		ctx.set(RoutingTable.class, r);
	}

	@Override
	public ListenableFuture<NodeID> requestJoin(final NodeID admittingPeer) {
		l.info(String.format("Joining to RELOAD overlay %s with %s through %s in progress...", conf.getOverlayName(), boot.getLocalNodeId(), admittingPeer));
		Bootstrap connector = ctx.get(Bootstrap.class);
		MessageBuilder msgBuilder = ctx.get(MessageBuilder.class);
		MessageRouter router = ctx.get(MessageRouter.class);

		JoinRequest req = new JoinRequest(connector.getLocalNodeId(), connector.getJoinData());

		DestinationList dest = new DestinationList(ResourceID.valueOf(connector.getLocalNodeId().getData()));

		Message request = msgBuilder.newMessage(req, dest);

		request.setAttribute(Message.NEXT_HOP, admittingPeer);

		ListenableFuture<Message> joinAnsFut = router.sendRequestMessage(request);

		final SettableFuture<NodeID> joinFuture = SettableFuture.create();

		Futures.addCallback(joinAnsFut, new FutureCallback<Message>() {

			public void onSuccess(Message joinAns) {
				r.neighbors.add(admittingPeer);
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

	@MessageHandler(ContentType.JOIN_REQ)
	public void handleJoinRequest(final Message req) {

		Message ans = msgBuilder.newResponseMessage(req.getHeader(), new JoinAnswer("JOIN ANS".getBytes()));

		r.neighbors.add(((JoinRequest) req.getContent()).getJoiningNode());

		router.sendMessage(ans);

	}

	@MessageHandler(ContentType.LEAVE_REQ)
	public void handleLeaveRequest(Message req) {
		LeaveRequest leave = (LeaveRequest) req.getContent();
		NodeID leavingNode = leave.getLeavingNode();

		// Check sender id matches with the leaving node
		if (!req.getHeader().getSenderId().equals(leavingNode)) {
			router.sendMessage(msgBuilder.newResponseMessage(req.getHeader(), new Error(ErrorType.FORBITTEN, "Leaving node doesn't match with sender ID")));
			return;
		}

		NodeID prevHop = req.getAttribute(Message.PREV_HOP);

		// Check neighbor id matches with the leaving node
		if (!prevHop.equals(leavingNode)) {
			router.sendMessage(msgBuilder.newResponseMessage(req.getHeader(), new Error(ErrorType.FORBITTEN, "Leaving node is not a neighbor node")));
			return;
		}

		r.neighbors.remove(leavingNode);
	}

	@Subscribe
	public void handleConnectionEvent(ConnectionStatusEvent e) {
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
	public ListenableFuture<NodeID> requestLeave(final NodeID neighborNode) {
		Message leaveMessage = msgBuilder.newMessage(new LeaveRequest(boot.getLocalNodeId(), new byte[0]), new DestinationList(msgBuilder.getWildcard()));
		leaveMessage.setAttribute(Message.NEXT_HOP, neighborNode);
		ListenableFuture<Message> ansFut = router.sendRequestMessage(leaveMessage);

		final SettableFuture<NodeID> leaveOutcome = SettableFuture.create();

		Futures.addCallback(ansFut, new FutureCallback<Message>() {

			@Override
			public void onSuccess(Message result) {
				isJoined = false;
				leaveOutcome.set(neighborNode);
			}

			@Override
			public void onFailure(Throwable t) {
				leaveOutcome.setException(t);
			}
		});

		return leaveOutcome;
	}

	@CompStop
	private void stop() {
		sendLeaveAndClose();
	}

	private void sendLeaveAndClose() {
		for (NodeID n : r.neighbors) {
			requestLeave(n);
		}
	}

	private class TestRouting implements RoutingTable {

		private final SortedSet<NodeID> neighbors = new TreeSet<NodeID>();

		@Override
		public Set<NodeID> getNextHops(RoutableID destination) {
			if (neighbors.isEmpty())
				return Collections.emptySet();

			int minDinstance = Integer.MAX_VALUE;

			NodeID singleNextHop = getCloserId(destination, neighbors);

			for (NodeID nodeId : neighbors) {
				int tmp = getDistance(destination, nodeId);
				if (tmp <= minDinstance) {
					minDinstance = tmp;
					singleNextHop = nodeId;
				}
			}

			return Collections.singleton(singleNextHop);
		}
	}

	@Override
	public ListenableFuture<NodeID> requestUpdate(NodeID neighborNode) {
		// TODO Auto-generated method stub
		return null;
	}
}
