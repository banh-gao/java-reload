package com.github.reload.net.codecs.header;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.EnumSet;
import com.github.reload.net.codecs.Codec;
import com.github.reload.net.codecs.Codec.ReloadCodec;

/**
 * A routable identitier that can be used as destination for the resource based
 * routing algorithm
 * 
 */
@ReloadCodec(RoutableIDCodec.class)
public abstract class RoutableID implements Comparable<RoutableID> {

	public enum DestinationType {
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

	public abstract byte[] getData();

	public abstract DestinationType getType();

	protected static byte[] hexToByte(String str) {
		byte[] bytes = new byte[str.length() / 2];
		for (int i = 0; i < bytes.length; i++) {
			bytes[i] = (byte) Integer.parseInt(str.substring(2 * i, 2 * i + 2), 16);
		}
		return bytes;
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
		return this.getClass().getSimpleName() + '[' + Codec.hexDump(getData()) + ']';
	}

	/**
	 * @return True if this id is the wildcard id, false otherwise
	 */
	public boolean isWildcard() {
		return (getType() == DestinationType.NODEID) && new BigInteger(getData()).equals(BigInteger.ZERO);
	}
}
