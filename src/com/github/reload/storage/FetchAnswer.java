package com.github.reload.storage;

import java.util.List;

public class FetchAnswer extends QueryAnswer<FetchResponse> {

	public FetchAnswer(Context context, UnsignedByteBuffer buf) throws UnknownKindException {
		super(context, buf);
	}

	public FetchAnswer(Context context, List<FetchResponse> responses) {
		super(context, responses);
	}

	@Override
	public ContentType getType() {
		return ContentType.FETCH_ANS;
	}

	@Override
	protected FetchResponse decodeResponse(Context context, UnsignedByteBuffer buf) throws UnknownKindException {
		return new FetchResponse(context, buf);
	}
}
