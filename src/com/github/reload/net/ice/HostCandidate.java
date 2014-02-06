package com.github.reload.net.ice;

import java.util.ArrayList;
import net.sf.jReload.message.UnsignedByteBuffer;

public class HostCandidate extends IceCandidate {

	public HostCandidate() {
	}

	public HostCandidate(IPAddressPort addrPort) {
		this.addrPort = addrPort;
		extensions = new ArrayList<IceExtension>();
		foundation = new byte[]{1};
		overlayLink = OverlayLinkType.TLS_TCP_FH_NO_ICE;
		priority = 1;
	}

	@Override
	protected CandidateType getCandType() {
		return CandidateType.HOST;
	}

	@Override
	protected void implWriteTo(UnsignedByteBuffer buf) {
		// No specific data
	}
}
