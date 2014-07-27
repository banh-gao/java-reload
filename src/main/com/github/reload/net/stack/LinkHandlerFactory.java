package com.github.reload.net.stack;

import java.security.NoSuchAlgorithmException;
import com.github.reload.net.ice.HostCandidate.OverlayLinkType;

public class LinkHandlerFactory {

	public static LinkHandler getInstance(OverlayLinkType linkType) throws NoSuchAlgorithmException {
		switch (linkType) {
			case TLS_TCP_FH_NO_ICE :
				return new SRLinkHandler();
			default :
				throw new NoSuchAlgorithmException(String.format("No valid link protocol implementation for %s available", linkType));
		}
	}
}
