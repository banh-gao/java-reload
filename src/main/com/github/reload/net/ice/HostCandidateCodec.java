package com.github.reload.net.ice;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import java.util.ArrayList;
import com.github.reload.conf.Configuration;
import com.github.reload.net.encoders.Codec;
import com.github.reload.net.ice.HostCandidate.CandidateType;
import com.github.reload.net.ice.HostCandidate.OverlayLinkType;

class HostCandidateCodec extends Codec<HostCandidate> {

	private static final int FOUNDATION_LENGTH_FIELD = U_INT8;
	private static final int EXTENSIONS_LENGTH_FIELD = U_INT16;

	private final Codec<IPAddressPort> socketAddrCodec;
	private final Codec<IceExtension> iceExtCodec;

	private final Codec<PeerReflexiveCandidate> peerRefCodec;
	private final Codec<RelayCandidate> relayCodec;
	private final Codec<ServerReflexiveCandidate> serverRefCodec;

	public HostCandidateCodec(Configuration conf) {
		super(conf);
		socketAddrCodec = getCodec(IPAddressPort.class);
		iceExtCodec = getCodec(IceExtension.class);
		peerRefCodec = getCodec(PeerReflexiveCandidate.class);
		relayCodec = getCodec(RelayCandidate.class);
		serverRefCodec = getCodec(ServerReflexiveCandidate.class);
	}

	@Override
	public void encode(HostCandidate obj, ByteBuf buf, Object... params) throws CodecException {
		socketAddrCodec.encode(obj.addrPort, buf);
		buf.writeByte(obj.overlayLink.code);

		Field lenFld = allocateField(buf, FOUNDATION_LENGTH_FIELD);
		buf.writeBytes(obj.foundation);
		lenFld.updateDataLength();

		buf.writeInt((int) obj.priority);
		buf.writeByte(obj.getCandType().code);

		switch (obj.getCandType()) {
			case HOST :
				// Already encoded
				break;
			case PEER_REFLEXIVE :
				peerRefCodec.encode((PeerReflexiveCandidate) obj, buf);
				break;
			case RELAY :
				peerRefCodec.encode((PeerReflexiveCandidate) obj, buf);
				break;
			case SERVER_REFLEXIVE :
				peerRefCodec.encode((PeerReflexiveCandidate) obj, buf);
				break;
		}

		encodeExtensions(obj, buf);
	}

	private void encodeExtensions(HostCandidate obj, ByteBuf buf) throws CodecException {
		Field lenFld = allocateField(buf, EXTENSIONS_LENGTH_FIELD);

		for (IceExtension ex : obj.extensions) {
			iceExtCodec.encode(ex, buf);
		}

		lenFld.updateDataLength();
	}

	@Override
	public HostCandidate decode(ByteBuf buf, Object... params) throws CodecException {
		IPAddressPort addrPort = socketAddrCodec.decode(buf);

		OverlayLinkType overlayLink = OverlayLinkType.valueOf(buf.readByte());
		if (overlayLink == null)
			throw new DecoderException("Unknown overlay link type");

		ByteBuf foundData = readField(buf, FOUNDATION_LENGTH_FIELD);
		byte[] foundation = new byte[foundData.readableBytes()];
		foundData.readBytes(foundation);

		long priority = buf.readUnsignedInt();

		CandidateType candType = CandidateType.valueOf(buf.readByte());

		if (candType == null)
			throw new DecoderException("Unknown ICE candidate type");

		HostCandidate candidate = null;

		switch (candType) {
			case HOST :
				candidate = new HostCandidate(addrPort);
				// Already decoded
				break;
			case PEER_REFLEXIVE :
				candidate = peerRefCodec.decode(buf);
				break;
			case RELAY :
				candidate = relayCodec.decode(buf);
				break;
			case SERVER_REFLEXIVE :
				candidate = serverRefCodec.decode(buf);
				break;
		}

		assert (candidate != null);

		candidate.overlayLink = overlayLink;
		candidate.foundation = foundation;
		candidate.priority = priority;

		ByteBuf extData = readField(buf, EXTENSIONS_LENGTH_FIELD);
		candidate.extensions = new ArrayList<IceExtension>();

		while (extData.readableBytes() > 0) {
			candidate.extensions.add(iceExtCodec.decode(extData));
		}
		return candidate;
	}
}
