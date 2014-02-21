package com.github.reload.message;

import io.netty.buffer.ByteBuf;
import java.util.HashMap;
import java.util.Map;
import com.github.reload.Context;
import com.github.reload.message.NodeID.NodeIdCodec;
import com.github.reload.net.data.Codec;
import com.github.reload.net.data.ReloadCodec;

/**
 * The identifier of a node
 * 
 * @author Daniel Zozin <zdenial@gmx.com>
 * 
 */
@ReloadCodec(NodeIdCodec.class)
public class NodeID extends RoutableID {

	private static final Map<byte[], NodeID> INSTANCES = new HashMap<byte[], NodeID>();
	private final byte[] id;

	NodeID(byte[] id) {
		this.id = id;
	}

	public static NodeID valueOf(byte[] id) {
		NodeID node = INSTANCES.get(id);
		if (node == null) {
			node = new NodeID(id);
			INSTANCES.put(id, node);
		}
		return node;
	}

	public static NodeID valueOf(int idLength, byte[] id) {
		int i, j;
		if (id.length <= idLength) {
			i = idLength - id.length;
			j = 0;
		} else {
			i = 0;
			j = id.length - idLength;
		}

		byte[] paddedId = new byte[idLength];

		for (; i < idLength && j < id.length; i++, j++) {
			paddedId[i] = id[j];
		}

		return valueOf(paddedId);
	}

	/**
	 * Create a node-id from a hexadecimal string
	 * 
	 * @param hexID
	 */
	public static NodeID valueOf(String hexId) {
		return valueOf(hexToByte(hexId));
	}

	/**
	 * @param the
	 *            length in byte of the wildcard id
	 * @return the wildcard node-id of the specified length
	 */
	public static NodeID getWildcardId(int idLength) {
		return valueOf(new byte[idLength]);
	}

	@Override
	public byte[] getData() {
		return id;
	}

	@Override
	public DestinationType getType() {
		return DestinationType.NODEID;
	}

	public static class NodeIdCodec extends Codec<NodeID> {

		public NodeIdCodec(Context context) {
			super(context);
		}

		@Override
		public void encode(NodeID obj, ByteBuf buf, Object... params) throws com.github.reload.net.data.Codec.CodecException {
			buf.writeBytes(obj.id);
		}

		@Override
		public NodeID decode(ByteBuf buf, Object... params) throws com.github.reload.net.data.Codec.CodecException {
			// FIXME: get nodeid length from configuration
			int nodeidLength = 16;

			byte[] id = new byte[nodeidLength];
			buf.readBytes(id);

			return valueOf(id);
		}
	}
}