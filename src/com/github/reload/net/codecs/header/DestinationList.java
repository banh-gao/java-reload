package com.github.reload.net.codecs.header;

import io.netty.buffer.ByteBuf;
import java.util.AbstractList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import com.github.reload.components.ComponentsContext;
import com.github.reload.net.codecs.Codec;
import com.github.reload.net.codecs.Codec.ReloadCodec;
import com.github.reload.net.codecs.header.DestinationList.DestinationListCodec;

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

	@Override
	public boolean add(RoutableID e) {
		return list.add(e);
	}

	@Override
	public void add(int index, RoutableID element) {
		list.add(index, element);
	}

	@Override
	public RoutableID set(int index, RoutableID element) {
		return list.set(index, element);
	}

	@Override
	public RoutableID get(int index) {
		return list.get(index);
	}

	@Override
	public int size() {
		return list.size();
	}

	@Override
	public RoutableID remove(int index) {
		return list.remove(index);
	}

	static class DestinationListCodec extends Codec<DestinationList> {

		private final Codec<RoutableID> rouIdCodec;

		public DestinationListCodec(ComponentsContext ctx) {
			super(ctx);
			rouIdCodec = getCodec(RoutableID.class);
		}

		@Override
		public void encode(DestinationList obj, ByteBuf buf, Object... params) throws com.github.reload.net.codecs.Codec.CodecException {
			for (RoutableID d : obj) {
				rouIdCodec.encode(d, buf);
			}
		}

		@Override
		public DestinationList decode(ByteBuf buf, Object... params) throws com.github.reload.net.codecs.Codec.CodecException {
			DestinationList out = new DestinationList();

			while (buf.readableBytes() > 0) {
				RoutableID id = rouIdCodec.decode(buf);
				out.add(id);
			}

			buf.release();

			return out;
		}

	}
}
