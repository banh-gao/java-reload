package com.github.reload.net.pipeline.handlers;

import java.security.NoSuchAlgorithmException;
import com.github.reload.net.ice.IceCandidate.OverlayLinkType;

public class LinkHandlerFactory {

	public static LinkHandler getInstance(OverlayLinkType linkType) throws NoSuchAlgorithmException {
		switch (linkType) {
			case TLS_TCP_FH_NO_ICE :
				return new SRLinkHandler();
			default :
				throw new NoSuchAlgorithmException("No valid link protocol implementation available");
		}
	}

}
