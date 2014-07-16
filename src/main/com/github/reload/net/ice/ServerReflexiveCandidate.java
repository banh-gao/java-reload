package com.github.reload.net.ice;

import com.github.reload.net.encoders.Codec.ReloadCodec;
import com.github.reload.net.ice.RelayCandidate.RelayCandidateCodec;

@ReloadCodec(RelayCandidateCodec.class)
public class ServerReflexiveCandidate extends RelayCandidate {

	public ServerReflexiveCandidate(IPAddressPort addrPort) {
		super(addrPort);
	}

	@Override
	protected CandidateType getCandType() {
		return CandidateType.SERVER_REFLEXIVE;
	}
}
