package com.github.reload;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import com.github.reload.components.ComponentsContext.CompStart;
import com.github.reload.components.ComponentsRepository.Component;
import com.github.reload.net.connections.ConnectionManager.ConnectionStatusEvent;
import com.github.reload.net.connections.ConnectionManager.ConnectionStatusEvent.Type;
import com.github.reload.net.encoders.header.NodeID;
import com.github.reload.net.encoders.header.ResourceID;
import com.github.reload.net.encoders.header.RoutableID;
import com.github.reload.net.encoders.secBlock.HashAlgorithm;
import com.github.reload.routing.RoutingTable;
import com.github.reload.routing.TopologyPlugin;
import com.google.common.eventbus.Subscribe;

@Component(TopologyPlugin.class)
public class TestPlugin implements TopologyPlugin {

	@Component
	private Bootstrap boot;

	private final TestRouting r = new TestRouting();

	@CompStart
	private void start() {
		r.neighbors.add(boot.getLocalNodeId());
	}

	@Subscribe
	private void handleConnectionEvent(ConnectionStatusEvent e) {
		if (e.type == Type.ACCEPTED || e.type == Type.ESTABLISHED)
			r.neighbors.add(e.connection.getNodeId());
		else if (e.type == Type.CLOSED)
			r.neighbors.remove(e.connection.getNodeId());
	}

	@Override
	public int getResourceIdLength() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getDistance(RoutableID source, RoutableID dest) {
		return 0;
	}

	@Override
	public HashAlgorithm getHashAlgorithm() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResourceID getResourceId(byte[] resourceIdentifier) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T extends RoutableID> T getCloserId(RoutableID destination, Collection<T> ids) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public RoutingTable getRoutingTable() {
		return r;
	}

	@Override
	public boolean isThisPeerResponsible(RoutableID destinationId) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isThisPeerResponsible(RoutableID destinationId, Collection<? extends NodeID> excludedNeighbors) {
		// TODO Auto-generated method stub
		return false;
	}

	private class TestRouting implements RoutingTable {

		private final SortedSet<NodeID> neighbors = new TreeSet<NodeID>();

		@Override
		public Set<NodeID> getNextHops(RoutableID destination) {
			return getNextHops(destination, Collections.<NodeID>emptyList());
		}

		@Override
		public Set<NodeID> getNextHops(RoutableID destination, Collection<? extends NodeID> excludedIds) {
			if (neighbors.isEmpty())
				return Collections.emptySet();

			int minDinstance = Integer.MAX_VALUE;

			NodeID singleNextHop = getCloserId(destination, neighbors);

			for (NodeID nodeId : neighbors) {
				if (excludedIds.contains(nodeId)) {
					continue;
				}
				int tmp = getDistance(destination, nodeId);
				if (tmp <= minDinstance) {
					minDinstance = tmp;
					singleNextHop = nodeId;
				}
			}

			return Collections.singleton(singleNextHop);
		}
	}
}
