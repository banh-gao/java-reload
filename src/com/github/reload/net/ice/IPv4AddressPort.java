package com.github.reload.net.ice;

import java.net.InetAddress;
import java.net.UnknownHostException;
import net.sf.jReload.message.DecodingException;
import net.sf.jReload.message.EncUtils;
import net.sf.jReload.message.UnsignedByteBuffer;

public class IPv4AddressPort extends IPAddressPort {

	private static final int ADDR_LENGTH = EncUtils.U_INT32;

	public IPv4AddressPort(InetAddress addr, int port) {
		super(addr, port);
	}

	public IPv4AddressPort(UnsignedByteBuffer buf) {
		super(parseAddr(buf), parsePort(buf));
	}

	private static InetAddress parseAddr(UnsignedByteBuffer buf) {
		byte[] tmpAddr = new byte[IPv4AddressPort.ADDR_LENGTH];
		buf.getRaw(tmpAddr);
		try {
			return InetAddress.getByAddress(tmpAddr);
		} catch (UnknownHostException e) {
			throw new DecodingException("Invalid IPv4 address");
		} catch (ClassCastException e) {
			throw new DecodingException("Invalid IPv4 address");
		}
	}

	public static int parsePort(UnsignedByteBuffer buf) {
		return buf.getSigned16();
	}

	@Override
	protected AddressType getAddressType() {
		return AddressType.IPv4;
	}

	@Override
	protected void implWriteTo(UnsignedByteBuffer buf) {
		buf.putRaw(getAddress().getAddress());
		buf.putUnsigned16(getPort());
	}
}
