package com.github.reload.net.ice;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.EnumSet;
import com.github.reload.message.ReloadCodec;

@ReloadCodec(IPAddressPortCodec.class)
public abstract class IPAddressPort extends InetSocketAddress {

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

		byte code;

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

	public static IPAddressPort create(InetAddress addr, int port) {
		if (addr instanceof Inet4Address)
			return new IPv4AddressPort(addr, port);
		else if (addr instanceof Inet6Address)
			return new IPv6AddressPort(addr, port);
		else
			throw new IllegalArgumentException("Unknown address type");
	}

	public static IPAddressPort create(InetSocketAddress socketAddr) {
		return create(socketAddr.getAddress(), socketAddr.getPort());
	}

	protected abstract AddressType getAddressType();

	@Override
	public String toString() {
		return getAddress().getHostAddress() + ":" + getPort();
	}
}
