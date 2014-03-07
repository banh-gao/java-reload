package com.github.reload.message;

import java.security.cert.Certificate;
import com.github.reload.Context;
import com.github.reload.Context.Component;
import com.github.reload.crypto.CryptoHelper;
import com.github.reload.message.content.ConfigUpdateRequest;
import com.github.reload.message.content.ConfigUpdateRequest.ConfigUpdateType;
import com.github.reload.message.errors.IncompatibleOverlayException;
import com.github.reload.net.data.Message;

/**
 * Creates new messages to send for a specific connection
 * 
 */
public class MessageBuilder implements Component {

	@Override
	public void compStart() {

	}

	/**
	 * Build a message by creating a message header for the passed destination
	 * list
	 * 
	 * @return the message ready to be send
	 */
	public Message newMessage(Content content, DestinationList destList) {
		if (content == null || destList == null)
			throw new NullPointerException();

		if (destList.size() == 0)
			throw new IllegalArgumentException("Invalid destination list");

		byte protoVersion = context.getPeerInfo().getProtocolVersion();

		Header header = Header.createHeader(context, localNeighbor, protoVersion, destList);

		return newMessage(header, content);
	}

	/**
	 * Build a message with the passed content and header, the sender is added
	 * at the end of the via list
	 * 
	 * @return the message ready to be send
	 */
	public Message newMessage(Header header, Content content) {
		NodeID senderID = context.getLocalId();
		header.getViaList().add(senderID);

		if (context.getLocalCert().getNodeIds().size() > 1)
			return getMultipleNodeIdMessage(senderID, header, content);

		return getSingleNodeIdMessage(header, content);
	}

	public Message newConfigMessage(ConfigUpdateType type, byte[] xmlConfiguration, DestinationList destinationList) {

		NeighborNode localNeighbor = context.getLoopbackNeighbor();

		Header header = Header.createHeader(context, localNeighbor, context.getPeerStatus().getProtocolVersion(), destinationList);
		header.setConfigurationSequence(0xffff);
		ConfigUpdateRequest confUpdate = new ConfigUpdateRequest(type, xmlConfiguration);
		return newMessage(header, confUpdate);
	}

	private Message getSingleNodeIdMessage(Header header, Content content) {
		CryptoHelper cryptoHelper = context.getComponent(Context.CRYPTO_HELPER);
		HashAlgorithm certHashAlg = cryptoHelper.getCertHashAlg();
		Certificate localCertificate = cryptoHelper.getLocalCertificate().getOriginalCertificate();
		SignerIdentity localIdentity = SignerIdentity.singleIdIdentity(certHashAlg, localCertificate);

		return buildMessage(localIdentity, header, content);
	}

	private Message getMultipleNodeIdMessage(NodeID senderID, Header header, Content content) {
		CryptoHelper cryptoHelper = context.getComponent(Context.CRYPTO_HELPER);
		HashAlgorithm certHashAlg = cryptoHelper.getCertHashAlg();
		Certificate localCertificate = cryptoHelper.getLocalCertificate().getOriginalCertificate();
		SignerIdentity localIdentity = SignerIdentity.multipleIdIdentity(certHashAlg, localCertificate, senderID);

		return buildMessage(localIdentity, header, content);
	}

	private Message buildMessage(SignerIdentity localIdentity, Header header, Content content) {
		return new Message(header, content, localIdentity, context);
	}

	/**
	 * Build a response message to a request message, the sender node-id is
	 * derived from the destination id contained in the original request header
	 * 
	 * @return the response message ready to be send
	 * @throws IncompatibleOverlayException
	 *             if the response message exceeds the maximum response length
	 *             specified in the request message
	 */
	public Message newResponseMessage(Header requestHeader, Content responseContent) {
		byte protoVersion = context.getPeerStatus().getProtocolVersion();

		Header responseHeader = Header.getResponseHeader(context, protoVersion, requestHeader);

		Message response = newMessage(responseHeader, responseContent);

		return response;
	}
}