package com.github.reload.net.ice;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import com.github.reload.net.codecs.Codec.ReloadCodec;
import com.github.reload.net.stack.LinkHandler;
import com.github.reload.net.stack.SRLinkHandler;

@ReloadCodec(HostCandidateCodec.class)
public class HostCandidate {

	public enum OverlayLinkType {
		DTLS_UDP_SR((byte) 1, "DTLS", true, null),
		DTLS_UDP_SR_NO_ICE((byte) 3, "DTLS", false, null),
		TLS_TCP_FH_NO_ICE((byte) 4, "TLS", false, SRLinkHandler.class);

		final byte code;
		final String linkProtocol;
		boolean useICE;
		Class<? extends LinkHandler> handler;

		OverlayLinkType(byte code, String linkProtocol, boolean useICE, Class<? extends LinkHandler> handler) {
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

		public Class<? extends LinkHandler> getHandler() {
			return handler;
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

		final byte code;

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

	protected final IPAddressPort addrPort;
	protected OverlayLinkType overlayLink;
	protected byte[] foundation;
	protected long priority;
	protected List<IceExtension> extensions;

	protected HostCandidate(IPAddressPort addrPort) {
		this.addrPort = addrPort;
		extensions = new ArrayList<IceExtension>();
		foundation = new byte[]{1};
		overlayLink = OverlayLinkType.TLS_TCP_FH_NO_ICE;
		priority = 1;
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

	protected CandidateType getCandType() {
		return CandidateType.HOST;
	}

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
		HostCandidate other = (HostCandidate) obj;
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
