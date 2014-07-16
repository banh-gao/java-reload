package com.github.reload.net.encoders.content;

import io.netty.buffer.ByteBuf;
import com.github.reload.Configuration;
import com.github.reload.net.encoders.Codec;
import com.github.reload.net.encoders.Codec.ReloadCodec;
import com.github.reload.net.encoders.content.UpdateAnswer.UpdateAnswerCodec;

@ReloadCodec(UpdateAnswerCodec.class)
public class UpdateAnswer extends Content {

	private final byte[] overlayData;

	public UpdateAnswer(byte[] overlayData) {
		this.overlayData = overlayData;
	}

	public byte[] getOverlayData() {
		return overlayData;
	}

	@Override
	public final ContentType getType() {
		return ContentType.UPDATE_ANS;
	}

	public static class UpdateAnswerCodec extends Codec<UpdateAnswer> {

		public UpdateAnswerCodec(Configuration conf) {
			super(conf);
		}

		@Override
		public void encode(UpdateAnswer obj, ByteBuf buf, Object... params) throws com.github.reload.net.encoders.Codec.CodecException {
			buf.writeBytes(obj.overlayData);
		}

		@Override
		public UpdateAnswer decode(ByteBuf buf, Object... params) throws com.github.reload.net.encoders.Codec.CodecException {
			byte[] data = new byte[buf.readableBytes()];
			buf.readBytes(data);
			return new UpdateAnswer(data);
		}

	}

}
