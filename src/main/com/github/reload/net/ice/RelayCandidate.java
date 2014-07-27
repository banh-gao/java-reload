package com.github.reload.net.ice;

import io.netty.buffer.ByteBuf;
import java.net.InetSocketAddress;
import com.github.reload.conf.Configuration;
import com.github.reload.net.encoders.Codec;
import com.github.reload.net.encoders.Codec.ReloadCodec;
import com.github.reload.net.ice.RelayCandidate.RelayCandidateCodec;

@ReloadCodec(RelayCandidateCodec.class)
public class RelayCandidate extends HostCandidate {

	private IPAddressPort relayAddrPort;

	public RelayCandidate(IPAddressPort addrPort) {
		super(addrPort);
		relayAddrPort = addrPort;
	}

	public void setRelayAddrPort(InetSocketAddress relayAddrPort) {
		this.relayAddrPort = IPAddressPort.create(relayAddrPort);
	}

	public InetSocketAddress getRelayAddrPort() {
		return relayAddrPort;
	}

	@Override
	protected CandidateType getCandType() {
		return CandidateType.RELAY;
	}

	static class RelayCandidateCodec extends Codec<RelayCandidate> {

		private final Codec<IPAddressPort> sockAddrCodec;

		public RelayCandidateCodec(Configuration conf) {
			super(conf);
			sockAddrCodec = getCodec(IPAddressPort.class);
		}

		@Override
		public void encode(RelayCandidate obj, ByteBuf buf, Object... params) throws com.github.reload.net.encoders.Codec.CodecException {
			sockAddrCodec.encode(obj.relayAddrPort, buf);
		}

		@Override
		public RelayCandidate decode(ByteBuf buf, Object... params) throws com.github.reload.net.encoders.Codec.CodecException {
			return new RelayCandidate(sockAddrCodec.decode(buf));
		}
	}
}
