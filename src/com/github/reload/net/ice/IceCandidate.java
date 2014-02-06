package com.github.reload.net.ice;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import net.sf.jReload.message.DecodingException;
import net.sf.jReload.message.EncUtils;
import net.sf.jReload.message.UnsignedByteBuffer;
import net.sf.jReload.message.UnsignedByteBuffer.Field;

public abstract class IceCandidate {

	private static final int FOUNDATION_LENGTH_FIELD = EncUtils.U_INT8;
	private static final int EXTENSIONS_LENGTH_FIELD = EncUtils.U_INT16;

	public enum OverlayLinkType {
		DTLS_UDP_SR((byte) 1, "DTLS", true),
		DTLS_UDP_SR_NO_ICE((byte) 3, "DTLS", false),
		TLS_TCP_FH_NO_ICE((byte) 4, "TLS", false);

		private final byte code;
		private final String linkProtocol;
		private boolean useICE;

		OverlayLinkType(byte code, String linkProtocol, boolean useICE) {
			this.code = code;
			this.linkProtocol = linkProtocol;
			this.useICE = useICE;
		}

		public byte getCode() {
			return code;
		}

		public String getLinkProtocol() {
			return linkProtocol;
		}

		public boolean isUseICE() {
			return useICE;
		}

		public static OverlayLinkType valueOf(byte code) {
			for (OverlayLinkType t : EnumSet.allOf(OverlayLinkType.class))
				if (t.code == code)
					return t;
			return null;
		}
	}

	enum CandidateType {
		HOST((byte) 1),
		SERVER_REFLEXIVE((byte) 2),
		PEER_REFLEXIVE((byte) 3),
		RELAY((byte) 4);

		private final byte code;

		CandidateType(byte code) {
			this.code = code;
		}

		public byte getCode() {
			return code;
		}

		public static CandidateType valueOf(byte code) {
			for (CandidateType t : EnumSet.allOf(CandidateType.class))
				if (t.code == code)
					return t;
			return null;
		}
	}

	protected IPAddressPort addrPort;
	protected OverlayLinkType overlayLink;
	protected byte[] foundation;
	protected long priority;
	protected List<IceExtension> extensions;

	protected IceCandidate() {
	}

	public static IceCandidate parse(UnsignedByteBuffer buf) {
		IPAddressPort addrPort = IPAddressPort.parse(buf);

		OverlayLinkType overlayLink = OverlayLinkType.valueOf(buf.getRaw8());
		if (overlayLink == null)
			throw new DecodingException("Unknown overlay link type");

		int fondLength = buf.getLengthValue(FOUNDATION_LENGTH_FIELD);
		byte[] foundation = new byte[fondLength];
		buf.getRaw(foundation);

		long priority = buf.getSigned32();

		CandidateType candType = CandidateType.valueOf(buf.getRaw8());

		if (candType == null)
			throw new DecodingException("Unknown ICE candidate type");

		IceCandidate candidate = null;

		switch (candType) {
			case HOST :
				candidate = new HostCandidate();
				break;
			case PEER_REFLEXIVE :
				candidate = new PeerReflexiveCandidate(buf);
				break;
			case RELAY :
				candidate = new RelayCandidate(buf);
				break;
			case SERVER_REFLEXIVE :
				candidate = new ServerReflexiveCandidate(buf);
				break;
		}

		assert (candidate != null);

		candidate.addrPort = addrPort;
		candidate.overlayLink = overlayLink;
		candidate.foundation = foundation;
		candidate.priority = priority;

		int extLength = buf.getLengthValue(IceCandidate.EXTENSIONS_LENGTH_FIELD);
		candidate.extensions = new ArrayList<IceExtension>();
		while (extLength > 0) {
			IceExtension extension = new IceExtension(buf);
			candidate.extensions.add(extension);
			extLength -= extension.getLength();
		}

		return candidate;
	}

	public void writeTo(UnsignedByteBuffer buf) {
		addrPort.writeTo(buf);
		buf.putRaw8(overlayLink.code);
		Field lenFld = buf.allocateLengthField(FOUNDATION_LENGTH_FIELD);
		buf.putRaw(foundation);
		lenFld.setEncodedLength(buf.getConsumedFrom(lenFld.getNextPosition()));
		buf.putUnsigned32(priority);
		buf.putRaw8(getCandType().code);
		implWriteTo(buf);
		writeExtensions(buf);
	}

	public InetSocketAddress getSocketAddress() {
		return addrPort;
	}

	public void setFoundation(byte[] foundation) {
		this.foundation = foundation;
	}

	public byte[] getFoundation() {
		return foundation;
	}

	public void setPriority(int priority) {
		this.priority = priority;
	}

	public long getPriority() {
		return priority;
	}

	public void setOverlayLinkType(OverlayLinkType overlayLink) {
		this.overlayLink = overlayLink;
	}

	public OverlayLinkType getOverlayLinkType() {
		return overlayLink;
	}

	public void setExtensions(List<IceExtension> extensions) {
		this.extensions = extensions;
	}

	public List<IceExtension> getExtensions() {
		return extensions;
	}

	private void writeExtensions(UnsignedByteBuffer buf) {
		Field lenFld = buf.allocateLengthField(EXTENSIONS_LENGTH_FIELD);

		for (IceExtension ex : extensions) {
			ex.writeTo(buf);
		}

		lenFld.setEncodedLength(buf.getConsumedFrom(lenFld.getNextPosition()));
	}

	protected abstract CandidateType getCandType();

	protected abstract void implWriteTo(UnsignedByteBuffer buf);

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((addrPort == null) ? 0 : addrPort.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		IceCandidate other = (IceCandidate) obj;
		if (addrPort == null) {
			if (other.addrPort != null)
				return false;
		} else if (!addrPort.equals(other.addrPort))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "IceCandidate [addrPort=" + addrPort + ", overlayLink=" + overlayLink + ", foundation=" + foundation + ", priority=" + priority + ", extensions=" + extensions + "]";
	}

}
