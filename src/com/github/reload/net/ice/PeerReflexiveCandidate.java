package com.github.reload.net.ice;

import net.sf.jReload.message.UnsignedByteBuffer;

public class PeerReflexiveCandidate extends RelayCandidate {

	public PeerReflexiveCandidate(UnsignedByteBuffer buf) {
		super(buf);
	}

	@Override
	protected CandidateType getCandType() {
		return CandidateType.PEER_REFLEXIVE;
	}
}
