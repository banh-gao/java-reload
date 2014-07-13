package com.github.reload.net.ice;

import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import com.github.reload.Configuration;
import com.github.reload.message.Codec;
import com.github.reload.message.ReloadCodec;
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

		public HostCandidateCodec(Configuration conf) {
			super(conf);
		}

		@Override
		public void encode(HostCandidate obj, ByteBuf buf, Object... params) throws com.github.reload.message.Codec.CodecException {
		}

		@Override
		public HostCandidate decode(ByteBuf buf, Object... params) throws com.github.reload.message.Codec.CodecException {
			return new HostCandidate();
		}
	}
}
