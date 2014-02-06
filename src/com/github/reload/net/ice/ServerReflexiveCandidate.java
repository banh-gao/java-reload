package com.github.reload.net.ice;

import net.sf.jReload.message.UnsignedByteBuffer;

public class ServerReflexiveCandidate extends RelayCandidate {

	public ServerReflexiveCandidate(UnsignedByteBuffer buf) {
		super(buf);
	}

	@Override
	protected CandidateType getCandType() {
		return CandidateType.SERVER_REFLEXIVE;
	}
}
