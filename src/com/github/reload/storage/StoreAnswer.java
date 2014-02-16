package com.github.reload.storage;

import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import com.github.reload.Context;
import com.github.reload.message.Content;
import com.github.reload.message.ContentType;
import com.github.reload.net.data.Codec;
import com.github.reload.net.data.ReloadCodec;
import com.github.reload.storage.StoreAnswer.StoreAnswerCodec;

@ReloadCodec(StoreAnswerCodec.class)
public class StoreAnswer extends Content {

	private final List<StoreResponse> responses;

	public StoreAnswer(List<StoreResponse> responses) {
		this.responses = responses;
	}

	public List<StoreResponse> getResponses() {
		return responses;
	}

	@Override
	public ContentType getType() {
		return ContentType.STORE_ANS;
	}

	public static class StoreAnswerCodec extends Codec<StoreAnswer> {

		private static final int RESPONSES_LENGTH_FIELD = U_INT16;

		private final Codec<StoreResponse> storeRespCodec;

		public StoreAnswerCodec(Context context) {
			super(context);
			storeRespCodec = getCodec(StoreResponse.class);
		}

		@Override
		public void encode(StoreAnswer obj, ByteBuf buf) throws CodecException {
			Field lenFld = allocateField(buf, RESPONSES_LENGTH_FIELD);

			for (StoreResponse r : obj.responses) {
				storeRespCodec.encode(r, buf);
			}

			lenFld.updateDataLength();
		}

		@Override
		public StoreAnswer decode(ByteBuf buf) throws CodecException {
			List<StoreResponse> responses = new ArrayList<StoreResponse>();

			ByteBuf respData = readField(buf, RESPONSES_LENGTH_FIELD);

			while (respData.readableBytes() > 0) {
				responses.add(storeRespCodec.decode(buf));
			}

			respData.release();

			return new StoreAnswer(responses);
		}

	}

}
