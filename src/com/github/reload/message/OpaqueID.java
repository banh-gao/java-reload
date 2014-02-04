package com.github.reload.message;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.EncoderException;
import com.github.reload.net.data.CodecUtils;
import com.github.reload.net.data.CodecUtils.Field;

/**
 * An opaque id used to substitute other ids by the local peer (also known as
 * compressed id)
 * 
 * @author Daniel Zozin <zdenial@gmx.com>
 * 
 */
public class OpaqueID extends RoutableID {

	private static final int OPAQUE_LENGTH_FIELD = CodecUtils.U_INT8;

	private final byte[] id;

	private OpaqueID(byte[] id) {
		this.id = id;
	}

	public static OpaqueID valueOf(byte[] id) {
		return new OpaqueID(id);
	}

	public static OpaqueID valueOf(ByteBuf buf) {
		ByteBuf data = CodecUtils.readData(buf, OPAQUE_LENGTH_FIELD);
		byte[] id = new byte[data.readableBytes()];
		data.readBytes(id);
		return valueOf(id);
	}

	@Override
	public byte[] getData() {
		return id;
	}

	/**
	 * Write this opaque as an opaque destination
	 * 
	 * @param buf
	 * @throws IllegalStateException
	 *             if the opaque id is not a valid opaque destination value
	 *             (uint16 with top bit setted)
	 */
	public void writeAsOpaqueDestinationTo(ByteBuf buf) {
		if (id.length != CodecUtils.U_INT16 || (id[0] & OPAQUE_DEST_MASK) != OPAQUE_DEST_MASK)
			throw new IllegalStateException("Invalid opaque-id for opaque destination, top bit must be setted");

		buf.writeBytes(id);
	}

	@Override
	public DestinationType getType() {
		return DestinationType.OPAQUEID;
	}

	@Override
	public void implEncode(ByteBuf buf) throws EncoderException {
		Field lenFld = CodecUtils.allocateField(buf, OPAQUE_LENGTH_FIELD);
		buf.writeBytes(id);
		lenFld.updateDataLength();
	}
}
