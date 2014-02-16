package com.github.reload.message.content;

import com.github.reload.message.RoutableID;

public class RouteQueryRequest extends MessageContent {

	private static final int DATA_LENGTH_FIELD = EncUtils.U_INT16;

	private final boolean sendUpdate;
	private final RoutableID destination;
	private final byte[] data;

	public RouteQueryRequest(RoutableID destId, byte[] data, boolean sendUpdate) {
		this.sendUpdate = sendUpdate;
		destination = destId;
		this.data = data;
	}

	public RouteQueryRequest(UnsignedByteBuffer buf) {
		sendUpdate = buf.getRaw8() > 0;
		destination = RoutableID.parseFromDestination(buf);
		int length = buf.getLengthValue(DATA_LENGTH_FIELD);
		data = new byte[length];
		buf.getRaw(data);
	}

	@Override
	protected void implWriteTo(UnsignedByteBuffer buf) {
		buf.putRaw8((byte) (sendUpdate ? 1 : 0));
		destination.writeAsDestinationTo(buf);
		Field lenFld = buf.allocateLengthField(DATA_LENGTH_FIELD);
		buf.putRaw(data);
		lenFld.setEncodedLength(buf.getConsumedFrom(lenFld.getNextPosition()));
	}

	@Override
	public ContentType getType() {
		return ContentType.ROUTE_QUERY_REQ;
	}

	public boolean isSendUpdate() {
		return sendUpdate;
	}

	public RoutableID getDestination() {
		return destination;
	}

	public byte[] getData() {
		return data;
	}
}
