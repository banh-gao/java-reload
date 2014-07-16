package com.github.reload.net.encoders.content.storage;

import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import com.github.reload.Configuration;
import com.github.reload.net.encoders.Codec;
import com.github.reload.net.encoders.Codec.ReloadCodec;
import com.github.reload.net.encoders.content.Content;
import com.github.reload.net.encoders.content.ContentType;
import com.github.reload.net.encoders.content.storage.StoreAnswer.StoreAnswerCodec;

@ReloadCodec(StoreAnswerCodec.class)
public class StoreAnswer extends Content {

	private final List<StoreKindResponse> responses;

	public StoreAnswer(List<StoreKindResponse> responses) {
		this.responses = responses;
	}

	public List<StoreKindResponse> getResponses() {
		return responses;
	}

	@Override
	public ContentType getType() {
		return ContentType.STORE_ANS;
	}

	public static class StoreAnswerCodec extends Codec<StoreAnswer> {

		private static final int RESPONSES_LENGTH_FIELD = U_INT16;

		private final Codec<StoreKindResponse> storeRespCodec;

		public StoreAnswerCodec(Configuration conf) {
			super(conf);
			storeRespCodec = getCodec(StoreKindResponse.class);
		}

		@Override
		public void encode(StoreAnswer obj, ByteBuf buf, Object... params) throws CodecException {
			Field lenFld = allocateField(buf, RESPONSES_LENGTH_FIELD);

			for (StoreKindResponse r : obj.responses) {
				storeRespCodec.encode(r, buf);
			}

			lenFld.updateDataLength();
		}

		@Override
		public StoreAnswer decode(ByteBuf buf, Object... params) throws CodecException {
			List<StoreKindResponse> responses = new ArrayList<StoreKindResponse>();

			ByteBuf respData = readField(buf, RESPONSES_LENGTH_FIELD);

			while (respData.readableBytes() > 0) {
				responses.add(storeRespCodec.decode(buf));
			}

			respData.release();

			return new StoreAnswer(responses);
		}

	}

}
