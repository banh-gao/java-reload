package com.github.reload.message;

import io.netty.buffer.ByteBuf;
import java.util.HashMap;
import java.util.Map;

/**
 * The identifier of a node
 * 
 * @author Daniel Zozin <zdenial@gmx.com>
 * 
 */
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

	public static NodeID valueOf(ByteBuf buf) {
		byte[] id = new byte[buf.readableBytes()];
		buf.readBytes(id);
		return valueOf(id);
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

	@Override
	public void implEncode(ByteBuf buf) {
		buf.writeBytes(id);
	}
}