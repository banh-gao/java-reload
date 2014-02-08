package com.github.reload.net.ice;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import java.util.ArrayList;
import com.github.reload.Context;
import com.github.reload.net.data.Codec;
import com.github.reload.net.ice.IceCandidate.CandidateType;
import com.github.reload.net.ice.IceCandidate.OverlayLinkType;

public class IceCandidateCodec extends Codec<IceCandidate> {

	private static final int FOUNDATION_LENGTH_FIELD = U_INT8;
	private static final int EXTENSIONS_LENGTH_FIELD = U_INT16;

	private final Codec<IPAddressPort> socketAddrCodec;
	private final Codec<IceExtension> iceExtCodec;

	private final Codec<HostCandidate> hostCandCodec;
	private final Codec<PeerReflexiveCandidate> peerRefCodec;
	private final Codec<RelayCandidate> relayCodec;
	private final Codec<ServerReflexiveCandidate> serverRefCodec;

	public IceCandidateCodec(Context context) {
		super(context);
		socketAddrCodec = getCodec(IPAddressPort.class, context);
		iceExtCodec = getCodec(IceExtension.class, context);
		hostCandCodec = getCodec(HostCandidate.class, context);
		peerRefCodec = getCodec(PeerReflexiveCandidate.class, context);
		relayCodec = getCodec(RelayCandidate.class, context);
		serverRefCodec = getCodec(ServerReflexiveCandidate.class, context);
	}

	@Override
	public void encode(IceCandidate obj, ByteBuf buf) throws CodecException {
		socketAddrCodec.encode(obj.addrPort, buf);
		buf.writeByte(obj.overlayLink.code);

		Field lenFld = allocateField(buf, FOUNDATION_LENGTH_FIELD);
		buf.writeBytes(obj.foundation);
		lenFld.updateDataLength();

		buf.writeInt((int) obj.priority);
		buf.writeByte(obj.getCandType().code);

		switch (obj.getCandType()) {
			case HOST :
				hostCandCodec.encode((HostCandidate) obj, buf);
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

	private void encodeExtensions(IceCandidate obj, ByteBuf buf) throws CodecException {
		Field lenFld = allocateField(buf, EXTENSIONS_LENGTH_FIELD);

		for (IceExtension ex : obj.extensions) {
			iceExtCodec.encode(ex, buf);
		}

		lenFld.updateDataLength();
	}

	@Override
	public IceCandidate decode(ByteBuf buf) throws CodecException {
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

		IceCandidate candidate = null;

		switch (candType) {
			case HOST :
				candidate = hostCandCodec.decode(buf);
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

		candidate.addrPort = addrPort;
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
