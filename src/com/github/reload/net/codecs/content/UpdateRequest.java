package com.github.reload.net.codecs.content;

import io.netty.buffer.ByteBuf;
import com.github.reload.components.ComponentsContext;
import com.github.reload.net.codecs.Codec;
import com.github.reload.net.codecs.Codec.ReloadCodec;
import com.github.reload.net.codecs.content.UpdateRequest.UpdateRequestCodec;

@ReloadCodec(UpdateRequestCodec.class)
public class UpdateRequest extends Content {

	private final byte[] overlayData;

	public UpdateRequest(byte[] overlayData) {
		this.overlayData = overlayData;
	}

	public byte[] getOverlayData() {
		return overlayData;
	}

	@Override
	public final ContentType getType() {
		return ContentType.UPDATE_REQ;
	}

	static class UpdateRequestCodec extends Codec<UpdateRequest> {

		public UpdateRequestCodec(ComponentsContext ctx) {
			super(ctx);
		}

		@Override
		public void encode(UpdateRequest obj, ByteBuf buf, Object... params) throws com.github.reload.net.codecs.Codec.CodecException {
			buf.writeBytes(obj.overlayData);
		}

		@Override
		public UpdateRequest decode(ByteBuf buf, Object... params) throws com.github.reload.net.codecs.Codec.CodecException {
			byte[] data = new byte[buf.readableBytes()];
			buf.readBytes(data);
			return new UpdateRequest(data);
		}

	}

}
