package com.github.reload.net.encoders.header;

import io.netty.buffer.ByteBuf;
import java.util.HashMap;
import java.util.Map;
import com.github.reload.conf.Configuration;
import com.github.reload.net.encoders.Codec;
import com.github.reload.net.encoders.Codec.ReloadCodec;
import com.github.reload.net.encoders.header.NodeID.NodeIdCodec;

/**
 * The identifier of a node
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

	static class NodeIdCodec extends Codec<NodeID> {

		private static final int NODE_ID_LENGTH = 16;

		public NodeIdCodec(Configuration conf) {
			super(conf);
		}

		@Override
		public void encode(NodeID obj, ByteBuf buf, Object... params) throws CodecException {
			if (obj.id.length != NODE_ID_LENGTH)
				throw new CodecException("Invalid NodeID length");
			buf.writeBytes(obj.id);
		}

		@Override
		public NodeID decode(ByteBuf buf, Object... params) throws com.github.reload.net.encoders.Codec.CodecException {
			byte[] id = new byte[NODE_ID_LENGTH];
			buf.readBytes(id);

			return valueOf(id);
		}
	}
}