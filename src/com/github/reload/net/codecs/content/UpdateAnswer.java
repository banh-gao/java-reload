package com.github.reload.net.codecs.content;

import io.netty.buffer.ByteBuf;
import dagger.ObjectGraph;
import com.github.reload.net.codecs.Codec;
import com.github.reload.net.codecs.Codec.ReloadCodec;
import com.github.reload.net.codecs.content.UpdateAnswer.UpdateAnswerCodec;

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

	static class UpdateAnswerCodec extends Codec<UpdateAnswer> {

		public UpdateAnswerCodec(ObjectGraph ctx) {
			super(ctx);
		}

		@Override
		public void encode(UpdateAnswer obj, ByteBuf buf, Object... params) throws com.github.reload.net.codecs.Codec.CodecException {
			buf.writeBytes(obj.overlayData);
		}

		@Override
		public UpdateAnswer decode(ByteBuf buf, Object... params) throws com.github.reload.net.codecs.Codec.CodecException {
			byte[] data = new byte[buf.readableBytes()];
			buf.readBytes(data);
			return new UpdateAnswer(data);
		}

	}

}
