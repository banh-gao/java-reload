package com.github.reload.net.ice;

import com.github.reload.net.data.ReloadCodec;
import com.github.reload.net.ice.RelayCandidate.RelayCandidateCodec;

@ReloadCodec(RelayCandidateCodec.class)
public class PeerReflexiveCandidate extends RelayCandidate {

	public PeerReflexiveCandidate(IPAddressPort addrPort) {
		super(addrPort);
	}

	@Override
	protected CandidateType getCandType() {
		return CandidateType.PEER_REFLEXIVE;
	}
}
