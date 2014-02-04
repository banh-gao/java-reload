package com.github.reload.message.content;

import net.sf.jReload.message.ContentType;
import net.sf.jReload.message.EncUtils;
import net.sf.jReload.message.MessageContent;
import net.sf.jReload.message.UnsignedByteBuffer;
import net.sf.jReload.message.UnsignedByteBuffer.Field;

public class PingRequest extends MessageContent {

	private static final int PADDING_LENGTH_FIELD = EncUtils.U_INT16;
	private final int payloadLength;

	public PingRequest(UnsignedByteBuffer buf) {
		int length = buf.getLengthValue(PADDING_LENGTH_FIELD);
		byte[] padding = new byte[length];
		buf.getRaw(padding);
		payloadLength = length;
	}

	public PingRequest() {
		payloadLength = 0;
	}

	public PingRequest(int payloadLength) {
		this.payloadLength = payloadLength;
	}

	public int getPayloadLength() {
		return payloadLength;
	}

	@Override
	protected void implWriteTo(UnsignedByteBuffer buf) {
		Field lenFld = buf.allocateLengthField(PADDING_LENGTH_FIELD);
		buf.putRaw(new byte[payloadLength]);
		lenFld.setEncodedLength(buf.getConsumedFrom(lenFld.getNextPosition()));
	}

	@Override
	public ContentType getType() {
		return ContentType.PING_REQ;
	}

}
