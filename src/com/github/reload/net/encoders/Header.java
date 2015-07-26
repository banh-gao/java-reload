package com.github.reload.net.encoders;

import io.netty.buffer.ByteBuf;
import io.netty.util.AttributeKey;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import com.github.reload.net.encoders.Codec.ReloadCodec;
import com.github.reload.net.encoders.header.DestinationList;
import com.github.reload.net.encoders.header.ForwardingOption;
import com.github.reload.net.encoders.header.NodeID;
import com.github.reload.net.encoders.header.RoutableID;
import com.github.reload.net.encoders.secBlock.HashAlgorithm;
import com.google.common.base.Objects;
import com.google.common.collect.Maps;

/**
 * RELOAD message header
 */
@ReloadCodec(HeaderCodec.class)
public class Header {

	final Map<AttributeKey<?>, Object> attributes = Maps.newHashMap();

	public static final AttributeKey<NodeID> PREV_HOP = AttributeKey.valueOf("PREV_HOP");
	public static final AttributeKey<ByteBuf> RAW_CONTENT = AttributeKey.valueOf("RAW_CONTENT");

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
	List<? extends ForwardingOption> forwardingOptions = new LinkedList<ForwardingOption>();

	int headerLength;
	int payloadLength;

	Header() {
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
	 *         NOTE: This value is reliable only after the message has been
	 *         properly authenticated
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
	public List<? extends ForwardingOption> getForwardingOptions() {
		return forwardingOptions;
	}

	public int getOverlayHash() {
		return overlayHash;
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this).add("transactionId", transactionId).add("isLastFragment", isLastFragment).add("fragmentOffset", fragmentOffset).add("maxResponseLength", maxResponseLength).add("configurationSequence", configurationSequence).add("overlayHash", overlayHash).add("version", version).add("ttl", ttl).add("viaList", viaList).add("destinationList", destinationList).add("forwardingOptions", forwardingOptions).add("headerLength", headerLength).add("payloadLength", payloadLength).toString();
	}

	@SuppressWarnings("unchecked")
	public <T> T setAttribute(AttributeKey<T> key, T value) {
		return (T) attributes.put(key, value);
	}

	@SuppressWarnings("unchecked")
	public <T> T getAttribute(AttributeKey<T> key) {
		Object a = attributes.get(key);
		if (a != null)
			return (T) a;
		return null;
	}

	public static class Builder {

		private static final SecureRandom transIdGen = new SecureRandom();

		boolean isLastFragment = true;
		int fragmentOffset = 0;

		long transactionId;

		int maxResponseLength = 0;
		int configurationSequence = 0;

		int overlayHash;
		short version;
		short ttl;

		DestinationList viaList = new DestinationList();
		DestinationList destinationList = new DestinationList();
		List<? extends ForwardingOption> forwardingOptions = new LinkedList<ForwardingOption>();

		int headerLength;
		int payloadLength;

		public static int overlayNameToHash(String name, HashAlgorithm hashAlg) {
			MessageDigest md;
			try {
				md = MessageDigest.getInstance(hashAlg.toString());
			} catch (NoSuchAlgorithmException e) {
				throw new RuntimeException(e);
			}
			byte[] hash = md.digest(name.getBytes());

			// Use the lowest 4 bytes of the generated hash
			ByteBuffer buf = ByteBuffer.allocate(hash.length);
			buf.put(hash);
			buf.position(buf.position() - 4);
			return buf.getInt();
		}

		public Builder setLastFragment(boolean isLastFragment) {
			this.isLastFragment = isLastFragment;
			return this;
		}

		public Builder setFragmentOffset(int fragmentOffset) {
			this.fragmentOffset = fragmentOffset;
			return this;
		}

		public Builder setTransactionId(long transactionId) {
			this.transactionId = transactionId;
			return this;
		}

		public Builder setMaxResponseLength(int maxResponseLength) {
			this.maxResponseLength = maxResponseLength;
			return this;
		}

		public Builder setConfigurationSequence(int configurationSequence) {
			this.configurationSequence = configurationSequence;
			return this;
		}

		public Builder setOverlayHash(int overlayHash) {
			this.overlayHash = overlayHash;
			return this;
		}

		public Builder setVersion(short version) {
			this.version = version;
			return this;
		}

		public Builder setTtl(short ttl) {
			this.ttl = ttl;
			return this;
		}

		public Builder setViaList(DestinationList viaList) {
			this.viaList = viaList;
			return this;
		}

		public Builder setDestinationList(DestinationList destinationList) {
			this.destinationList = destinationList;
			return this;
		}

		public Builder setForwardingOptions(List<? extends ForwardingOption> forwardingOptions) {
			this.forwardingOptions = forwardingOptions;
			return this;
		}

		public Header build() {
			Header h = new Header();
			h.configurationSequence = configurationSequence;
			h.destinationList = destinationList;
			h.forwardingOptions = forwardingOptions;
			h.fragmentOffset = fragmentOffset;
			h.isLastFragment = isLastFragment;
			h.maxResponseLength = maxResponseLength;
			h.overlayHash = overlayHash;
			if (h.transactionId == 0) {
				h.transactionId = transIdGen.nextLong();
			} else {
				h.transactionId = transactionId;
			}
			h.ttl = ttl;
			h.version = version;
			h.viaList = viaList;
			return h;
		}
	}

	public void toForward(NodeID lastHop) {
		ttl -= 1;
		if (!viaList.getDestination().equals(lastHop)) {
			viaList.add(lastHop);
		}

		if (destinationList.size() > 1) {
			destinationList.remove(0);
		}
	}

}
