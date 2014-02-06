package com.github.reload.net.ice;

import java.net.InetAddress;
import java.net.UnknownHostException;
import net.sf.jReload.message.DecodingException;
import net.sf.jReload.message.EncUtils;
import net.sf.jReload.message.UnsignedByteBuffer;

public class IPv6AddressPort extends IPAddressPort {

	private static final int ADDR_LENGTH = EncUtils.U_INT128;

	public IPv6AddressPort(InetAddress addr, int port) {
		super(addr, port);
	}

	public IPv6AddressPort(UnsignedByteBuffer buf) {
		super(parseAddr(buf), parsePort(buf));
	}

	private static InetAddress parseAddr(UnsignedByteBuffer buf) {
		byte[] tmpAddr = new byte[IPv6AddressPort.ADDR_LENGTH];
		buf.getRaw(tmpAddr);
		try {
			return InetAddress.getByAddress(tmpAddr);
		} catch (UnknownHostException e) {
			throw new DecodingException("Invalid IPv6 address");
		} catch (ClassCastException e) {
			throw new DecodingException("Invalid IPv6 address");
		}

	}

	private static int parsePort(UnsignedByteBuffer buf) {
		return buf.getSigned16();
	}

	@Override
	protected AddressType getAddressType() {
		return AddressType.IPv6;
	}

	@Override
	protected void implWriteTo(UnsignedByteBuffer buf) {
		buf.putRaw(getAddress().getAddress());
		buf.putUnsigned16(getPort());
	}
}
