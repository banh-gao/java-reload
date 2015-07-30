package com.github.reload.routing;

import javax.inject.Inject;
import javax.inject.Singleton;
import com.github.reload.net.codecs.header.DestinationList;
import com.github.reload.net.codecs.header.OpaqueID;

@Singleton
public class DefaultPathCompressor implements PathCompressor {

	@Inject
	public DefaultPathCompressor() {
	}

	@Override
	public OpaqueID compress(DestinationList list) {
		throw new UnsupportedOperationException();
	}

	@Override
	public DestinationList decompress(OpaqueID id) throws UnknownOpaqueIdException {
		throw new UnknownOpaqueIdException(id);
	}

}
