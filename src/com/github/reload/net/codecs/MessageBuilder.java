package com.github.reload.net.codecs;

import java.util.Collections;
import javax.inject.Inject;
import javax.inject.Singleton;
import com.github.reload.Overlay;
import com.github.reload.conf.Configuration;
import com.github.reload.crypto.CryptoHelper;
import com.github.reload.net.codecs.Header.Builder;
import com.github.reload.net.codecs.content.Content;
import com.github.reload.net.codecs.header.DestinationList;
import com.github.reload.net.codecs.header.NodeID;

@Singleton
public class MessageBuilder {

	@Inject
	Overlay overlay;

	@Inject
	Configuration conf;

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
		b.setOverlayHash(Builder.overlayNameToHash(conf.get(Configuration.OVERLAY_NAME), CryptoHelper.OVERLAY_HASHALG));
		b.setTtl(conf.get(Configuration.INITIAL_TTL));
		b.setMaxResponseLength(conf.get(Configuration.MAX_MESSAGE_SIZE));
		b.setVersion(Overlay.RELOAD_PROTOCOL_VERSION);
		Header header = b.build();

		return newMessage(header, content);
	}

	public NodeID getWildcard() {
		return NodeID.getWildcardId(conf.get(Configuration.NODE_ID_LENGTH));
	}

	private Message newMessage(Header header, Content content) {
		NodeID sender = overlay.getLocalNodeId();
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
		viaList.add(overlay.getLocalNodeId());

		// Reset TTL
		requestHeader.ttl = conf.get(Configuration.INITIAL_TTL);

		return msg;
	}
}
