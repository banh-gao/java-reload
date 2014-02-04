package com.github.reload.message;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.EnumSet;
import com.github.reload.net.data.CodecUtils;
import com.github.reload.net.data.CodecUtils.Field;

/**
 * A routable identitier that can be used as destination for the resource based
 * routing algorithm
 * 
 * @author Daniel Zozin <zdenial@gmx.com>
 * 
 */
public abstract class RoutableID implements Comparable<RoutableID> {

	protected enum DestinationType {
		NODEID((byte) 0x01), RESOURCEID((byte) 0x02), OPAQUEID((byte) 0x03);

		public final byte code;

		DestinationType(byte code) {
			this.code = code;
		}

		public static DestinationType valueOf(byte code) {
			for (DestinationType t : EnumSet.allOf(DestinationType.class))
				if (t.code == code)
					return t;
			return null;
		}
	}

	private static final int DEST_LENGTH_FIELD = CodecUtils.U_INT8;

	protected static final int OPAQUE_DEST_MASK = 0x80;

	protected abstract byte[] getData();

	public abstract DestinationType getType();

	public abstract void implEncode(ByteBuf buf);

	/**
	 * Parse the data in the buffer as a Destination structure
	 * 
	 * @param buf
	 * @return
	 */
	public static RoutableID parseFromDestination(ByteBuf buf) {
		byte firstByte = buf.readByte();

		if ((firstByte & OPAQUE_DEST_MASK) == OPAQUE_DEST_MASK) {
			byte[] opaqueDest = new byte[2];
			buf.readBytes(opaqueDest);
			return parseOpaqueDestination(opaqueDest);
		}
		DestinationType type = DestinationType.valueOf(firstByte);

		if (type == null)
			throw new DecoderException("Unsupported destination type");

		ByteBuf dataBuf = CodecUtils.readData(buf, DEST_LENGTH_FIELD);
		switch (type) {
			case NODEID :
				return NodeID.valueOf(dataBuf);
			case RESOURCEID :
				return ResourceID.valueOf(dataBuf);
			case OPAQUEID :
				return OpaqueID.valueOf(dataBuf);
			default :
				throw new DecoderException("Unsupported destination type");
		}
	}

	private static OpaqueID parseOpaqueDestination(byte[] id) {
		if ((id[0] & OPAQUE_DEST_MASK) != OPAQUE_DEST_MASK)
			throw new DecoderException("Invalid opaque-id");
		return OpaqueID.valueOf(id);
	}

	protected static byte[] hexToByte(String str) {
		byte[] bytes = new byte[str.length() / 2];
		for (int i = 0; i < bytes.length; i++) {
			bytes[i] = (byte) Integer.parseInt(str.substring(2 * i, 2 * i + 2), 16);
		}
		return bytes;
	}

	/**
	 * Write this id as a Destination structure to the specified buffer
	 */
	public void writeAsDestinationTo(ByteBuf buf) {
		buf.writeByte(getType().code);

		Field lenFld = CodecUtils.allocateField(buf, DEST_LENGTH_FIELD);

		implEncode(buf);

		lenFld.updateDataLength();
	}

	@Override
	public int compareTo(RoutableID o) {
		return new BigInteger(1, getData()).compareTo(new BigInteger(1, o.getData()));
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(getData());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RoutableID other = (RoutableID) obj;
		if (!Arrays.equals(getData(), other.getData()))
			return false;
		return true;
	}

	public String toHexString() {
		return new BigInteger(getData()).toString(16);
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + '[' + CodecUtils.hexDump(getData()) + ']';
	}

	/**
	 * @return True if this id is the wildcard id, false otherwise
	 */
	public boolean isWildcard() {
		return new BigInteger(getData()).equals(BigInteger.ZERO);
	}
}
