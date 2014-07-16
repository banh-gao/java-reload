package com.github.reload.net.encoders.content;

import io.netty.buffer.ByteBuf;
import com.github.reload.Configuration;
import com.github.reload.net.encoders.Codec;
import com.github.reload.net.encoders.Codec.ReloadCodec;
import com.github.reload.net.encoders.content.UpdateRequest.UpdateRequestCodec;

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

		public UpdateRequestCodec(Configuration conf) {
			super(conf);
		}

		@Override
		public void encode(UpdateRequest obj, ByteBuf buf, Object... params) throws com.github.reload.net.encoders.Codec.CodecException {
			buf.writeBytes(obj.overlayData);
		}

		@Override
		public UpdateRequest decode(ByteBuf buf, Object... params) throws com.github.reload.net.encoders.Codec.CodecException {
			byte[] data = new byte[buf.readableBytes()];
			buf.readBytes(data);
			return new UpdateRequest(data);
		}

	}

}
