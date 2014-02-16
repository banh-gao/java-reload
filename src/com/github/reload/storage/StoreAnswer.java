package com.github.reload.storage;

import java.util.ArrayList;
import java.util.List;

public class StoreAnswer extends MessageContent {

	private static final int RESPONSES_LENGTH_FIELD = EncUtils.U_INT16;

	private final List<StoreResponse> responses;

	public StoreAnswer(List<StoreResponse> responses) {
		this.responses = responses;
	}

	public StoreAnswer(Context context, UnsignedByteBuffer buf) throws UnknownKindException {
		int length = buf.getLengthValue(StoreAnswer.RESPONSES_LENGTH_FIELD);

		responses = new ArrayList<StoreResponse>();

		int startPos = buf.position();

		while (buf.getConsumedFrom(startPos) < length) {
			StoreResponse resp = new StoreResponse(context, buf);
			responses.add(resp);
		}
	}

	public List<StoreResponse> getResponses() {
		return responses;
	}

	@Override
	protected void implWriteTo(UnsignedByteBuffer buf) {
		Field lenFld = buf.allocateLengthField(RESPONSES_LENGTH_FIELD);

		for (StoreResponse r : responses) {
			r.writeTo(buf);
		}

		lenFld.setEncodedLength(buf.getConsumedFrom(lenFld.getNextPosition()));
	}

	@Override
	public ContentType getType() {
		return ContentType.STORE_ANS;
	}

}
