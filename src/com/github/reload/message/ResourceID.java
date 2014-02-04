package com.github.reload.message;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.EncoderException;
import com.github.reload.net.data.CodecUtils;
import com.github.reload.net.data.CodecUtils.Field;

/**
 * The identifier of a resource
 * 
 * @author Daniel Zozin <zdenial@gmx.com>
 * 
 */
public final class ResourceID extends RoutableID {

	private static final int VALUE_LENGTH_FIELD = CodecUtils.U_INT8;

	private final byte[] id;

	private ResourceID(byte[] id) {
		this.id = id;
	}

	public static ResourceID valueOf(byte[] id) {
		return new ResourceID(id);
	}

	public static ResourceID valueOf(ByteBuf buf) {
		ByteBuf data = CodecUtils.readData(buf, ResourceID.VALUE_LENGTH_FIELD);
		byte[] id = new byte[data.readableBytes()];
		buf.readBytes(id);
		return valueOf(id);
	}

	public static ResourceID valueOf(String hexString) {
		return new ResourceID(hexToByte(hexString));
	}

	@Override
	public byte[] getData() {
		return id;
	}

	@Override
	public DestinationType getType() {
		return DestinationType.RESOURCEID;
	}

	@Override
	public void implEncode(ByteBuf buf) throws EncoderException {
		Field lenFld = CodecUtils.allocateField(buf, VALUE_LENGTH_FIELD);
		buf.writeBytes(id);
		lenFld.updateDataLength();
	}
}
