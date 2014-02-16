package com.github.reload.storage;

import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import com.github.reload.Context;
import com.github.reload.message.Content;
import com.github.reload.message.ContentType;
import com.github.reload.net.data.Codec;
import com.github.reload.net.data.ReloadCodec;
import com.github.reload.storage.FetchAnswer.FetchAnswerCodec;

@ReloadCodec(FetchAnswerCodec.class)
public class FetchAnswer extends Content {

	private final List<FetchKindResponse> responses;

	public FetchAnswer(List<FetchKindResponse> responses) {
		this.responses = responses;
	}

	@Override
	public ContentType getType() {
		return ContentType.FETCH_ANS;
	}

	public static class FetchAnswerCodec extends Codec<FetchAnswer> {

		private static final int RESPONSES_LENGTH_FIELD = U_INT32;

		private final Codec<FetchKindResponse> respCodec;

		public FetchAnswerCodec(Context context) {
			super(context);
			respCodec = getCodec(FetchKindResponse.class);
		}

		@Override
		public void encode(FetchAnswer obj, ByteBuf buf) throws com.github.reload.net.data.Codec.CodecException {
			Field lenFld = allocateField(buf, RESPONSES_LENGTH_FIELD);

			for (FetchKindResponse r : obj.responses) {
				respCodec.encode(r, buf);
			}

			lenFld.updateDataLength();
		}

		@Override
		public FetchAnswer decode(ByteBuf buf) throws com.github.reload.net.data.Codec.CodecException {
			ByteBuf resposeData = readField(buf, RESPONSES_LENGTH_FIELD);
			List<FetchKindResponse> responses = new ArrayList<FetchKindResponse>();

			while (resposeData.readableBytes() > 0) {
				responses.add(respCodec.decode(buf));
			}

			resposeData.release();

			return new FetchAnswer(responses);
		}
	}
}
