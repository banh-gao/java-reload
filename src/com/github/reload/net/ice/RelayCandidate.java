package com.github.reload.net.ice;

import java.net.InetSocketAddress;
import net.sf.jReload.message.UnsignedByteBuffer;

public class RelayCandidate extends IceCandidate {

	private IPAddressPort relayAddrPort;

	public RelayCandidate(UnsignedByteBuffer buf) {
		relayAddrPort = IPAddressPort.parse(buf);
	}

	@Override
	protected void implWriteTo(UnsignedByteBuffer buf) {
		relayAddrPort.writeTo(buf);
	}

	public void setRelayAddrPort(InetSocketAddress relayAddrPort) {
		this.relayAddrPort = IPAddressPort.create(relayAddrPort);
	}

	public InetSocketAddress getRelayAddrPort() {
		return relayAddrPort;
	}

	@Override
	protected CandidateType getCandType() {
		return CandidateType.RELAY;
	}
}
