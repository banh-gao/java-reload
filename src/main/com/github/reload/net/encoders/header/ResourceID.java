package com.github.reload.net.encoders.header;

import io.netty.buffer.ByteBuf;
import com.github.reload.Configuration;
import com.github.reload.net.encoders.Codec;
import com.github.reload.net.encoders.Codec.ReloadCodec;
import com.github.reload.net.encoders.header.ResourceID.ResourceIDCodec;

/**
 * The identifier of a resource
 * 
 */
@ReloadCodec(ResourceIDCodec.class)
public final class ResourceID extends RoutableID {

	private final byte[] id;

	private ResourceID(byte[] id) {
		this.id = id;
	}

	public static ResourceID valueOf(byte[] id) {
		return new ResourceID(id);
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

	static class ResourceIDCodec extends Codec<ResourceID> {

		public ResourceIDCodec(Configuration conf) {
			super(conf);
		}

		private static final int VALUE_LENGTH_FIELD = U_INT8;

		@Override
		public void encode(ResourceID obj, ByteBuf buf, Object... params) throws com.github.reload.net.encoders.Codec.CodecException {
			Field lenFld = allocateField(buf, VALUE_LENGTH_FIELD);
			buf.writeBytes(obj.id);
			lenFld.updateDataLength();
		}

		@Override
		public ResourceID decode(ByteBuf buf, Object... params) throws com.github.reload.net.encoders.Codec.CodecException {
			ByteBuf data = readField(buf, VALUE_LENGTH_FIELD);
			byte[] id = new byte[data.readableBytes()];
			data.readBytes(id);
			data.release();
			return valueOf(id);
		}
	}
}
