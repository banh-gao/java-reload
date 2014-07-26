package com.github.reload.net.encoders;

import java.security.cert.Certificate;
import com.github.reload.Components.Component;
import com.github.reload.ReloadConnector;
import com.github.reload.ReloadOverlay;
import com.github.reload.conf.Configuration;
import com.github.reload.crypto.CryptoHelper;
import com.github.reload.net.encoders.content.Content;
import com.github.reload.net.encoders.content.errors.IncompatibleOverlayException;
import com.github.reload.net.encoders.header.DestinationList;
import com.github.reload.net.encoders.header.Header;
import com.github.reload.net.encoders.header.Header.Builder;
import com.github.reload.net.encoders.header.NodeID;
import com.github.reload.net.encoders.secBlock.HashAlgorithm;
import com.github.reload.net.encoders.secBlock.SignerIdentity;

@Component(MessageBuilderFactory.COMPNAME)
public class MessageBuilderFactory {

	public static final String COMPNAME = "com.github.reload.net.encoders.MessageBuilderFactory";

	@Component
	private Configuration conf;
	@Component
	private ReloadConnector connector;
	@Component
	private CryptoHelper<?> cryptoHelper;

	public MessageBuilder newBuilder() {
		return new MessageBuilder();
	}

	public class MessageBuilder {

		MessageBuilder() {
		}

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

		/**
		 * Build a message with the passed content and header, the sender is
		 * added
		 * at the end of the via list
		 * 
		 * @return the message ready to be send
		 */
		public Message newMessage(Header header, Content content) {
			NodeID sender = connector.getLocalNodeId();
			header.getViaList().add(sender);

			return getSingleNodeIdMessage(header, content);
		}

		private Message getSingleNodeIdMessage(Header header, Content content) {
			HashAlgorithm certHashAlg = cryptoHelper.getCertHashAlg();
			Certificate localCertificate = cryptoHelper.getLocalCertificate();
			SignerIdentity localIdentity = SignerIdentity.singleIdIdentity(certHashAlg, localCertificate);

			return buildMessage(localIdentity, header, content);
		}

		private Message buildMessage(SignerIdentity localIdentity, Header header, Content content) {
			return new Message(header, content, null);
		}

		/**
		 * Build a response message to a request message, the sender node-id is
		 * derived from the destination id contained in the original request
		 * header
		 * 
		 * @return the response message ready to be send
		 * @throws IncompatibleOverlayException
		 *             if the response message exceeds the maximum response
		 *             length
		 *             specified in the request message
		 */
		public Message newResponseMessage(Header requestHeader, Content responseContent) {
			byte protoVersion = context.getPeerStatus().getProtocolVersion();

			Header responseHeader = Header.getResponseHeader(context, protoVersion, requestHeader);

			Message response = newMessage(responseHeader, responseContent);

			return response;
		}
	}
}
