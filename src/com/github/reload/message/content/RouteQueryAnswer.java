package com.github.reload.message.content;

import io.netty.buffer.ByteBuf;
import com.github.reload.Context;
import com.github.reload.message.Content;
import com.github.reload.message.ContentType;
import com.github.reload.message.content.RouteQueryAnswer.RouteQueryAnswerCodec;
import com.github.reload.net.data.Codec;
import com.github.reload.net.data.ReloadCodec;

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

	public static class RouteQueryAnswerCodec extends Codec<RouteQueryAnswer> {

		public RouteQueryAnswerCodec(Context context) {
			super(context);
		}

		@Override
		public void encode(RouteQueryAnswer obj, ByteBuf buf) throws com.github.reload.net.data.Codec.CodecException {
			buf.writeBytes(obj.overlayData);
		}

		@Override
		public RouteQueryAnswer decode(ByteBuf buf) throws com.github.reload.net.data.Codec.CodecException {
			byte[] data = new byte[buf.readableBytes()];
			buf.readBytes(data);
			return new RouteQueryAnswer(data);
		}

	}

}
