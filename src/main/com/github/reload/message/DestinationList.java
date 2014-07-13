package com.github.reload.message;

import java.util.AbstractList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import com.github.reload.message.Codec.ReloadCodec;

/**
 * A destination list used to specify the path between peers a message
 * must traverse in the RELOAD overlay
 */
@ReloadCodec(DestinationListCodec.class)
public class DestinationList extends AbstractList<RoutableID> {

	private final LinkedList<RoutableID> list;

	public DestinationList() {
		list = new LinkedList<RoutableID>();
	}

	/**
	 * Creates a destination list using the ids in given list in the order
	 * returned by the collection iterator
	 * 
	 * @param list
	 * @return
	 */
	public DestinationList(Collection<? extends RoutableID> destinations) {
		this();
		list.addAll(destinations);
	}

	/**
	 * Create a destination list from the passed ids in the given order
	 * 
	 * @param destinationId
	 * @return
	 */
	public DestinationList(RoutableID... destinationId) {
		this(Arrays.asList(destinationId));
	}

	public RoutableID getDestination() {
		return list.getLast();
	}

	public NodeID getNodeDestination() {
		RoutableID last = getDestination();
		if (last instanceof NodeID)
			return (NodeID) last;
		throw new IllegalArgumentException("Invalid node-id destination");
	}

	public boolean isNodeDestination() {
		return (getDestination() instanceof NodeID);
	}

	public boolean isResourceDestination() {
		return (getDestination() instanceof ResourceID);
	}

	public ResourceID getResourceDestination() {
		RoutableID last = getDestination();
		if (last instanceof ResourceID)
			return (ResourceID) last;
		throw new IllegalArgumentException("Invalid resource-id destination");
	}

	@Override
	public boolean add(RoutableID e) {
		return list.add(e);
	}

	@Override
	public RoutableID get(int index) {
		return list.get(index);
	}

	@Override
	public int size() {
		return list.size();
	}
}
