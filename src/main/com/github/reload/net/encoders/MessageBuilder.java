package com.github.reload.net.encoders;

import com.github.reload.Components.Component;
import com.github.reload.ReloadConnector;
import com.github.reload.ReloadOverlay;
import com.github.reload.conf.Configuration;
import com.github.reload.crypto.CryptoHelper;
import com.github.reload.net.encoders.content.Content;
import com.github.reload.net.encoders.header.DestinationList;
import com.github.reload.net.encoders.header.Header;
import com.github.reload.net.encoders.header.Header.Builder;
import com.github.reload.net.encoders.header.NodeID;

@Component(MessageBuilder.COMPNAME)
public class MessageBuilder {

	public static final String COMPNAME = "com.github.reload.net.encoders.MessageBuilder";

	@Component
	private Configuration conf;
	@Component
	private ReloadConnector connector;
	@Component
	private CryptoHelper<?> cryptoHelper;

	/**
	 * Build a message for the given content and destination.
	 */
	public Message newMessage(Content content, DestinationList destList) {
		if (content == null || destList == null)
			throw new NullPointerException();

		if (destList.size() == 0)
			throw new IllegalArgumentException("Invalid destination list");

		Builder b = new Header.Builder();

		b.setDestinationList(destList);
		b.setOverlayHash(Builder.overlayNameToHash(conf.getOverlayName(), CryptoHelper.OVERLAY_HASHALG));
		b.setTtl(conf.getInitialTTL());
		b.setMaxResponseLength(conf.getMaxMessageSize());
		b.setVersion(ReloadOverlay.RELOAD_PROTOCOL_VERSION);
		Header header = b.build();

		return newMessage(header, content);
	}

	private Message newMessage(Header header, Content content) {
		NodeID sender = connector.getLocalNodeId();
		header.getViaList().add(sender);

		return new Message(header, content, null);
	}

	/**
	 * Build a response message to a request message. The given header
	 * object is modified to be used in the response message
	 * 
	 * @return the response message ready to be send
	 */
	public Message newResponseMessage(Header requestHeader, Content responseContent) {
		requestHeader.toResponse();
		return newMessage(requestHeader, responseContent);

	}
}
