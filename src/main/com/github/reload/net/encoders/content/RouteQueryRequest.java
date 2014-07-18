package com.github.reload.net.encoders.content;

import io.netty.buffer.ByteBuf;
import com.github.reload.Configuration;
import com.github.reload.net.encoders.Codec;
import com.github.reload.net.encoders.Codec.ReloadCodec;
import com.github.reload.net.encoders.content.RouteQueryRequest.RouteQueryRequestCodec;
import com.github.reload.net.encoders.header.RoutableID;

@ReloadCodec(RouteQueryRequestCodec.class)
public class RouteQueryRequest extends Content {

	private final boolean sendUpdate;
	private final RoutableID destination;
	private final byte[] overlayData;

	public RouteQueryRequest(RoutableID destId, byte[] overlayData, boolean sendUpdate) {
		this.sendUpdate = sendUpdate;
		destination = destId;
		this.overlayData = overlayData;
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

	public byte[] getOverlayData() {
		return overlayData;
	}

	static class RouteQueryRequestCodec extends Codec<RouteQueryRequest> {

		private static final int DATA_LENGTH_FIELD = U_INT16;

		private final Codec<RoutableID> destCodec;

		public RouteQueryRequestCodec(Configuration conf) {
			super(conf);
			destCodec = getCodec(RoutableID.class);
		}

		@Override
		public void encode(RouteQueryRequest obj, ByteBuf buf, Object... params) throws CodecException {
			buf.writeByte(obj.sendUpdate ? 1 : 0);

			destCodec.encode(obj.destination, buf);

			Field lenFld = allocateField(buf, DATA_LENGTH_FIELD);
			buf.writeBytes(obj.overlayData);
			lenFld.updateDataLength();
		}

		@Override
		public RouteQueryRequest decode(ByteBuf buf, Object... params) throws CodecException {
			boolean sendUpdate = buf.readUnsignedByte() > 0;

			RoutableID destination = destCodec.decode(buf);

			ByteBuf overlayData = readField(buf, DATA_LENGTH_FIELD);
			byte[] data = new byte[overlayData.readableBytes()];
			overlayData.readBytes(data);
			overlayData.release();

			return new RouteQueryRequest(destination, data, sendUpdate);
		}

	}
}
