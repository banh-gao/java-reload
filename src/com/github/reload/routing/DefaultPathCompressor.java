package com.github.reload.routing;

import com.github.reload.net.encoders.header.DestinationList;
import com.github.reload.net.encoders.header.OpaqueID;

public class DefaultPathCompressor implements PathCompressor {

	@Override
	public OpaqueID compress(DestinationList list) {
		throw new UnsupportedOperationException();
	}

	@Override
	public DestinationList decompress(OpaqueID id) throws UnknownOpaqueIdException {
		throw new UnknownOpaqueIdException(id);
	}

}
