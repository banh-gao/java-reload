package com.github.reload.message;

import java.util.LinkedList;
import java.util.List;
import com.github.reload.net.data.CodecUtils;

/**
 * RELOAD message header
 */
public class Header {

	static {
		CodecFactory.registerCodec(Header.class, new HeaderCodec());
	}

	boolean isReloTokenValid;
	long transactionId;

	boolean isLastFragment = true;
	int fragmentOffset = 0;

	int maxResponseLength = 0;
	int configurationSequence = 0;

	int overlayHash;
	short version;
	short ttl;

	DestinationList viaList = new DestinationList();
	DestinationList destinationList = new DestinationList();
	List<ForwardingOption> forwardingOptions = new LinkedList<ForwardingOption>();

	int headerLength;
	int payloadLength;

	Header() {
	}

	public boolean hasValidToken() {
		return isReloTokenValid;
	}

	public int getPayloadLength() {
		return payloadLength;
	}

	public int getHeaderLength() {
		return headerLength;
	}

	/**
	 * @return the total message length: header + payload
	 */
	public int getMessageLength() {
		return getHeaderLength() + getPayloadLength();
	}

	/**
	 * @return the node-id of the last hop (first entry of the via list) (!!!NOT
	 *         AUTHENTICATED!!!). For authenticated previous hop id see
	 *         {@link Message#getSenderNeighbor()}
	 */
	public NodeID getPreviousHop() {
		if (viaList.size() == 0 || !(viaList.get(0) instanceof NodeID))
			return null;
		return (NodeID) viaList.get(0);
	}

	/**
	 * @return the sender id of this message (last entry of the via list)
	 *         (!!!NOT AUTHENTICATED!!!). For authenticated sender id see
	 *         {@link Message#getSenderId()}
	 */
	public NodeID getSenderId() {
		if (viaList.size() == 0 || !(viaList.get(0) instanceof NodeID))
			throw new IllegalArgumentException("Empty via list");
		return (NodeID) viaList.get(0);
	}

	/**
	 * @return the next hop this message must be forwarded (first entry of the
	 *         destination list)
	 */
	public RoutableID getNextHop() {
		if (destinationList.size() == 0)
			throw new IllegalArgumentException("Empty destination list");
		return destinationList.get(0);
	}

	/**
	 * @return the destination id of this message (last entry of the destination
	 *         list)
	 */
	public RoutableID getDestinationId() {
		if (destinationList.size() == 0)
			throw new IllegalStateException("Empty destination list");
		return destinationList.get(destinationList.size() - 1);
	}

	/**
	 * @return true if this is the last (or the only) fragment, false otherwise
	 */
	public boolean isLastFragment() {
		return isLastFragment;
	}

	/**
	 * @return the bytes offset from the beginning of message payload for this
	 *         fragment
	 */
	public int getFragmentOffset() {
		return fragmentOffset;
	}

	/**
	 * @return true if this is a message fragment, false if the message is not
	 *         fragmented
	 */
	public boolean isFragmented() {
		return isLastFragment() && getFragmentOffset() > 0;
	}

	public int getMaxResponseLength() {
		return maxResponseLength;
	}

	public void setConfigurationSequence(int configurationSequence) {
		this.configurationSequence = configurationSequence;
	}

	public int getConfigurationSequence() {
		return configurationSequence;
	}

	public short getVersion() {
		return version;
	}

	public short getTimeToLive() {
		return ttl;
	}

	public long getTransactionId() {
		return transactionId;
	}

	/**
	 * @return A reference to the header via list
	 */
	public DestinationList getViaList() {
		return viaList;
	}

	/**
	 * @return A reference to the header destination list
	 */
	public DestinationList getDestinationList() {
		return destinationList;
	}

	/**
	 * @return A reference to the header forwarding options
	 */
	public List<ForwardingOption> getForwardingOptions() {
		return forwardingOptions;
	}

	public int getOverlayHash() {
		return overlayHash;
	}

	@Override
	public String toString() {
		return "ForwardingHeader [overlayHash=" + CodecUtils.hexDump(overlayHash) + ", messageLength=" + getMessageLength() + ", headerLength=" + headerLength + ", payloadLength=" + payloadLength + ", ttl=" + ttl + ", transactionId=" + CodecUtils.hexDump(transactionId) + ", viaList=" + viaList + ", destinationList=" + destinationList + ", isLastFragment=" + isLastFragment + ", fragmentOffset=" + fragmentOffset + "]";
	}
}
