package com.github.reload.net.ice;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.EnumSet;
import net.sf.jReload.message.DecodingException;
import net.sf.jReload.message.EncUtils;
import net.sf.jReload.message.UnsignedByteBuffer;
import net.sf.jReload.message.UnsignedByteBuffer.Field;

public abstract class IPAddressPort extends InetSocketAddress {

	public static final int ADDR_LENGTH_FIELD = EncUtils.U_INT8;

	protected IPAddressPort(String hostname, int port) {
		super(hostname, port);
	}

	protected IPAddressPort(InetAddress addr, int port) {
		super(addr, port);
	}

	protected IPAddressPort(int port) {
		super(port);
	}

	enum AddressType {
		IPv4((byte) 1), IPv6((byte) 2);

		private byte code;

		private AddressType(byte code) {
			this.code = code;
		}

		public static AddressType valueOf(byte code) {
			for (AddressType t : EnumSet.allOf(AddressType.class))
				if (t.code == code)
					return t;
			return null;
		}
	}

	public static IPAddressPort parse(UnsignedByteBuffer buf) {
		AddressType type = AddressType.valueOf(buf.getRaw8());
		if (type == null)
			throw new DecodingException("Unknown address type");

		IPAddressPort out = null;
		@SuppressWarnings("unused")
		int length = buf.getLengthValue(ADDR_LENGTH_FIELD);

		switch (type) {
			case IPv4 :
				out = new IPv4AddressPort(buf);
				break;
			case IPv6 :
				out = new IPv6AddressPort(buf);
				break;
		}

		return out;
	}

	public static IPAddressPort create(InetAddress addr, int port) {
		if (addr instanceof Inet4Address)
			return new IPv4AddressPort(addr, port);
		else if (addr instanceof Inet6Address)
			return new IPv6AddressPort(addr, port);
		else
			throw new DecodingException("Unknown address type");
	}

	public static IPAddressPort create(InetSocketAddress socketAddr) {
		return create(socketAddr.getAddress(), socketAddr.getPort());
	}

	public void writeTo(UnsignedByteBuffer buf) {
		buf.putRaw8(getAddressType().code);
		Field lenFld = buf.allocateLengthField(ADDR_LENGTH_FIELD);

		implWriteTo(buf);

		lenFld.setEncodedLength(buf.getConsumedFrom(lenFld.getNextPosition()));
	}

	protected abstract void implWriteTo(UnsignedByteBuffer buf);

	protected abstract AddressType getAddressType();

	@Override
	public String toString() {

		return getAddress().getHostAddress() + ":" + getPort();
	}
}
