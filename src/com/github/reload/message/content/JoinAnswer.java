package com.github.reload.message.content;

import io.netty.buffer.ByteBuf;
import com.github.reload.Configuration;
import com.github.reload.message.Content;
import com.github.reload.message.ContentType;
import com.github.reload.message.content.JoinAnswer.JoinAnswerCodec;
import com.github.reload.net.data.Codec;
import com.github.reload.net.data.ReloadCodec;

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

	public static class JoinAnswerCodec extends Codec<JoinAnswer> {

		private static final int DATA_LENGTH_FIELD = U_INT16;

		public JoinAnswerCodec(Configuration conf) {
			super(conf);
		}

		@Override
		public void encode(JoinAnswer obj, ByteBuf buf, Object... params) throws com.github.reload.net.data.Codec.CodecException {
			Field lenFld = allocateField(buf, DATA_LENGTH_FIELD);
			buf.writeBytes(obj.overlayData);
			lenFld.updateDataLength();
		}

		@Override
		public JoinAnswer decode(ByteBuf buf, Object... params) throws com.github.reload.net.data.Codec.CodecException {
			ByteBuf overlayData = readField(buf, DATA_LENGTH_FIELD);
			byte[] data = new byte[overlayData.readableBytes()];
			buf.readBytes(data);
			buf.release();
			return new JoinAnswer(data);
		}

	}

}
