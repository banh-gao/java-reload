package com.github.reload.message.content;

import io.netty.buffer.ByteBuf;
import com.github.reload.Context;
import com.github.reload.message.Content;
import com.github.reload.message.ContentType;
import com.github.reload.message.content.UpdateRequest.UpdateRequestCodec;
import com.github.reload.net.data.Codec;
import com.github.reload.net.data.ReloadCodec;

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

	public static class UpdateRequestCodec extends Codec<UpdateRequest> {

		public UpdateRequestCodec(Context context) {
			super(context);
		}

		@Override
		public void encode(UpdateRequest obj, ByteBuf buf) throws com.github.reload.net.data.Codec.CodecException {
			buf.writeBytes(obj.overlayData);
		}

		@Override
		public UpdateRequest decode(ByteBuf buf) throws com.github.reload.net.data.Codec.CodecException {
			byte[] data = new byte[buf.readableBytes()];
			buf.readBytes(data);
			return new UpdateRequest(data);
		}

	}

}
