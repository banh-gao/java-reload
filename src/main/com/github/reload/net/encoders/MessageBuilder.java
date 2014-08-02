package com.github.reload.net.encoders;

import java.util.Collections;
import com.github.reload.Bootstrap;
import com.github.reload.Overlay;
import com.github.reload.components.ComponentsRepository.Component;
import com.github.reload.conf.Configuration;
import com.github.reload.crypto.CryptoHelper;
import com.github.reload.net.encoders.Header.Builder;
import com.github.reload.net.encoders.content.Content;
import com.github.reload.net.encoders.header.DestinationList;
import com.github.reload.net.encoders.header.NodeID;

@Component(MessageBuilder.class)
public class MessageBuilder {

	@Component
	private Configuration conf;
	@Component
	private Bootstrap connector;
	@Component
	private CryptoHelper<?> cryptoHelper;

	private NodeID wildcardId;

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
		b.setVersion(Overlay.RELOAD_PROTOCOL_VERSION);
		Header header = b.build();

		return newMessage(header, content);
	}

	public NodeID getWildcard() {
		if (wildcardId == null)
			wildcardId = NodeID.getWildcardId(conf.getNodeIdLength());
		return wildcardId;
	}

	private Message newMessage(Header header, Content content) {
		NodeID sender = connector.getLocalNodeId();
		header.getViaList().add(sender);

		return new Message(header, content, null);
	}

	/**
	 * Build a response message to a request message. The given header
	 * object is recycled and be used in the response message
	 * 
	 * @return the response message ready to be send
	 */
	public Message newResponseMessage(Header requestHeader, Content responseContent) {

		Message msg = new Message(requestHeader, responseContent, null);

		DestinationList viaList = requestHeader.getViaList();

		// Set destination list as the reverse of the via list
		DestinationList destList = requestHeader.getDestinationList();
		destList.clear();
		destList.addAll(viaList);
		Collections.reverse(destList);

		// Clear via list and add local node as first node
		viaList.clear();
		viaList.add(connector.getLocalNodeId());

		// Reset TTL
		requestHeader.ttl = conf.getInitialTTL();

		return msg;
	}
}
