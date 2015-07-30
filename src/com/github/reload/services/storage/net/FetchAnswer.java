package com.github.reload.services.storage.net;

import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import dagger.ObjectGraph;
import com.github.reload.net.codecs.Codec;
import com.github.reload.net.codecs.Codec.ReloadCodec;
import com.github.reload.net.codecs.content.Content;
import com.github.reload.net.codecs.content.ContentType;
import com.github.reload.services.storage.net.FetchAnswer.FetchAnswerCodec;

@ReloadCodec(FetchAnswerCodec.class)
public class FetchAnswer extends Content {

	private final List<FetchKindResponse> responses;

	public FetchAnswer(List<FetchKindResponse> responses) {
		this.responses = responses;
	}

	public List<FetchKindResponse> getResponses() {
		return responses;
	}

	@Override
	public ContentType getType() {
		return ContentType.FETCH_ANS;
	}

	static class FetchAnswerCodec extends Codec<FetchAnswer> {

		private static final int RESPONSES_LENGTH_FIELD = U_INT32;

		private final Codec<FetchKindResponse> respCodec;

		public FetchAnswerCodec(ObjectGraph ctx) {
			super(ctx);
			respCodec = getCodec(FetchKindResponse.class);
		}

		@Override
		public void encode(FetchAnswer obj, ByteBuf buf, Object... params) throws com.github.reload.net.codecs.Codec.CodecException {
			Field lenFld = allocateField(buf, RESPONSES_LENGTH_FIELD);

			for (FetchKindResponse r : obj.responses) {
				respCodec.encode(r, buf);
			}

			lenFld.updateDataLength();
		}

		@Override
		public FetchAnswer decode(ByteBuf buf, Object... params) throws com.github.reload.net.codecs.Codec.CodecException {
			ByteBuf resposeData = readField(buf, RESPONSES_LENGTH_FIELD);
			List<FetchKindResponse> responses = new ArrayList<FetchKindResponse>();

			while (resposeData.readableBytes() > 0) {
				responses.add(respCodec.decode(resposeData));
			}

			resposeData.release();

			return new FetchAnswer(responses);
		}
	}
}
