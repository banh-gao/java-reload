package com.github.reload.net.codecs.content;

import io.netty.buffer.ByteBuf;
import dagger.ObjectGraph;
import com.github.reload.net.codecs.Codec;
import com.github.reload.net.codecs.Codec.ReloadCodec;
import com.github.reload.net.codecs.content.JoinAnswer.JoinAnswerCodec;

@ReloadCodec(JoinAnswerCodec.class)
public class JoinAnswer extends Content {

	private final byte[] overlayData;

	public JoinAnswer(byte[] overlayData) {
		this.overlayData = overlayData;
	}

	public byte[] getOverlayData() {
		return overlayData;
	}

	@Override
	public final ContentType getType() {
		return ContentType.JOIN_ANS;
	}

	static class JoinAnswerCodec extends Codec<JoinAnswer> {

		private static final int DATA_LENGTH_FIELD = U_INT16;

		public JoinAnswerCodec(ObjectGraph ctx) {
			super(ctx);
		}

		@Override
		public void encode(JoinAnswer obj, ByteBuf buf, Object... params) throws com.github.reload.net.codecs.Codec.CodecException {
			Field lenFld = allocateField(buf, DATA_LENGTH_FIELD);
			buf.writeBytes(obj.overlayData);
			lenFld.updateDataLength();
		}

		@Override
		public JoinAnswer decode(ByteBuf buf, Object... params) throws com.github.reload.net.codecs.Codec.CodecException {
			ByteBuf overlayData = readField(buf, DATA_LENGTH_FIELD);
			byte[] data = new byte[overlayData.readableBytes()];
			overlayData.readBytes(data);
			overlayData.release();
			return new JoinAnswer(data);
		}

	}

}
