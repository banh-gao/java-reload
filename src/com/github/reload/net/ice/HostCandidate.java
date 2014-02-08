package com.github.reload.net.ice;

import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import com.github.reload.Context;
import com.github.reload.net.data.Codec;
import com.github.reload.net.data.ReloadCodec;
import com.github.reload.net.ice.HostCandidate.HostCandidateCodec;

@ReloadCodec(HostCandidateCodec.class)
public class HostCandidate extends IceCandidate {

	public HostCandidate() {
	}

	public HostCandidate(IPAddressPort addrPort) {
		this.addrPort = addrPort;
		extensions = new ArrayList<IceExtension>();
		foundation = new byte[]{1};
		overlayLink = OverlayLinkType.TLS_TCP_FH_NO_ICE;
		priority = 1;
	}

	@Override
	protected CandidateType getCandType() {
		return CandidateType.HOST;
	}

	public static class HostCandidateCodec extends Codec<HostCandidate> {

		public HostCandidateCodec(Context context) {
			super(context);
		}

		@Override
		public void encode(HostCandidate obj, ByteBuf buf) throws com.github.reload.net.data.Codec.CodecException {
		}

		@Override
		public HostCandidate decode(ByteBuf buf) throws com.github.reload.net.data.Codec.CodecException {
			return new HostCandidate();
		}
	}
}
