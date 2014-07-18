package com.github.reload.net.encoders.content;

import io.netty.buffer.ByteBuf;
import com.github.reload.Configuration;
import com.github.reload.net.encoders.Codec;
import com.github.reload.net.encoders.Codec.ReloadCodec;
import com.github.reload.net.encoders.content.RouteQueryAnswer.RouteQueryAnswerCodec;

@ReloadCodec(RouteQueryAnswerCodec.class)
public class RouteQueryAnswer extends Content {

	private final byte[] overlayData;

	public RouteQueryAnswer(byte[] overlayData) {
		this.overlayData = overlayData;
	}

	public byte[] getOverlayData() {
		return overlayData;
	}

	@Override
	public final ContentType getType() {
		return ContentType.ROUTE_QUERY_ANS;
	}

	static class RouteQueryAnswerCodec extends Codec<RouteQueryAnswer> {

		public RouteQueryAnswerCodec(Configuration conf) {
			super(conf);
		}

		@Override
		public void encode(RouteQueryAnswer obj, ByteBuf buf, Object... params) throws com.github.reload.net.encoders.Codec.CodecException {
			buf.writeBytes(obj.overlayData);
		}

		@Override
		public RouteQueryAnswer decode(ByteBuf buf, Object... params) throws com.github.reload.net.encoders.Codec.CodecException {
			byte[] data = new byte[buf.readableBytes()];
			buf.readBytes(data);
			return new RouteQueryAnswer(data);
		}

	}

}
