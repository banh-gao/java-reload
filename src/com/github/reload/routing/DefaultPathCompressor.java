package com.github.reload.routing;

import com.github.reload.components.ComponentsRepository.Component;
import com.github.reload.net.encoders.header.DestinationList;
import com.github.reload.net.encoders.header.OpaqueID;

@Component(value = PathCompressor.class, priority = 1)
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
