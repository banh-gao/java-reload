package com.github.reload.message;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import com.github.reload.Context;
import com.github.reload.message.RoutableID.DestinationType;
import com.github.reload.net.data.Codec;

public class RoutableIDCodec extends Codec<RoutableID> {

	private static final int DEST_LENGTH_FIELD = U_INT8;

	private static final int OPAQUE_DEST_MASK = 0x80;

	private final Codec<NodeID> nodeIdCodec;
	private final Codec<ResourceID> resIdCodec;
	private final Codec<OpaqueID> opaqueIdCodec;

	public RoutableIDCodec(Context context) {
		super(context);
		nodeIdCodec = getCodec(NodeID.class);
		resIdCodec = getCodec(ResourceID.class);
		opaqueIdCodec = getCodec(OpaqueID.class);
	}

	@Override
	public void encode(RoutableID obj, ByteBuf buf, Object... params) throws com.github.reload.net.data.Codec.CodecException {
		buf.writeByte(obj.getType().code);

		Field lenFld = allocateField(buf, DEST_LENGTH_FIELD);

		switch (obj.getType()) {
			case NODEID :
				nodeIdCodec.encode((NodeID) obj, buf);
				break;
			case RESOURCEID :
				resIdCodec.encode((ResourceID) obj, buf);
				break;
			case OPAQUEID :
				opaqueIdCodec.encode((OpaqueID) obj, buf);
				break;
			default :
				throw new DecoderException("Unsupported destination type");
		}

		lenFld.updateDataLength();
	}

	@Override
	public RoutableID decode(ByteBuf buf, Object... params) throws com.github.reload.net.data.Codec.CodecException {
		byte firstByte = buf.readByte();

		if ((firstByte & OPAQUE_DEST_MASK) == OPAQUE_DEST_MASK) {
			byte[] opaqueDest = new byte[2];
			buf.readBytes(opaqueDest);
			return parseOpaqueDestination(opaqueDest);
		}
		DestinationType type = DestinationType.valueOf(firstByte);

		if (type == null)
			throw new DecoderException("Unsupported destination type");

		ByteBuf dataBuf = readField(buf, DEST_LENGTH_FIELD);
		RoutableID out = null;
		switch (type) {
			case NODEID :
				out = nodeIdCodec.decode(dataBuf);
				break;
			case RESOURCEID :
				out = resIdCodec.decode(dataBuf);
				break;
			case OPAQUEID :
				out = opaqueIdCodec.decode(dataBuf);
				break;
			default :
				throw new DecoderException("Unsupported destination type");
		}

		return out;
	}

	private static OpaqueID parseOpaqueDestination(byte[] id) {
		if ((id[0] & OPAQUE_DEST_MASK) != OPAQUE_DEST_MASK)
			throw new DecoderException("Invalid opaque-id");
		return OpaqueID.valueOf(id);
	}

}