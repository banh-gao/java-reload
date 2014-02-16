package com.github.reload.storage;

import java.util.List;

public class StatAnswer extends QueryAnswer<StatResponse> {

	public StatAnswer(Context context, UnsignedByteBuffer buf) throws UnknownKindException {
		super(context, buf);
	}

	public StatAnswer(Context context, List<StatResponse> responses) {
		super(context, responses);
	}

	@Override
	public ContentType getType() {
		return ContentType.STAT_ANS;
	}

	@Override
	protected StatResponse decodeResponse(Context context, UnsignedByteBuffer buf) throws UnknownKindException {
		return new StatResponse(context, buf);
	}
}
