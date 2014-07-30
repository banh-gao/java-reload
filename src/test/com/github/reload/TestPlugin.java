package com.github.reload;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import com.github.reload.components.ComponentsContext.CompStart;
import com.github.reload.components.ComponentsRepository.Component;
import com.github.reload.net.encoders.header.NodeID;
import com.github.reload.net.encoders.header.ResourceID;
import com.github.reload.net.encoders.header.RoutableID;
import com.github.reload.net.encoders.secBlock.HashAlgorithm;
import com.github.reload.routing.PathCompressor;
import com.github.reload.routing.RoutingTable;
import com.github.reload.routing.TopologyPlugin;

@Component(TopologyPlugin.class)
public class TestPlugin implements TopologyPlugin {

	@Component
	private Bootstrap boot;

	private final TestRouting r = new TestRouting();

	@CompStart
	private void start() {
		r.addNeighbor(boot.getLocalNodeId());
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
	public void onNeighborConnected(NodeID newNode, boolean updateRequested) {
		r.addNeighbor(newNode);
		// TODO Auto-generated method stub
	}

	@Override
	public void onNeighborDisconnected(NodeID node) {
		r.removeNeighbor(node);
		// TODO Auto-generated method stub

	}

	@Override
	public void onTransmissionFailed(NodeID node) {
		// TODO Auto-generated method stub

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

	@Override
	public PathCompressor getPathCompressor() {
		// TODO Auto-generated method stub
		return null;
	}

	private class TestRouting implements RoutingTable {

		private final SortedSet<NodeID> neighbors = new TreeSet<NodeID>();

		public void addNeighbor(NodeID nodeId) {
			neighbors.add(nodeId);
		}

		public void removeNeighbor(NodeID node) {
			neighbors.remove(node);
		}

		@Override
		public Set<NodeID> getNextHops(RoutableID destination) {
			return getNextHops(destination, new ArrayList<NodeID>());
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
