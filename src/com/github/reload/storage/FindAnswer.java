package com.github.reload.storage;

import java.util.ArrayList;
import java.util.List;

public class FindAnswer extends MessageContent {

	private static final int LIST_LENGTH_FIELD = EncUtils.U_INT16;

	private final List<FindKindData> data;

	public FindAnswer(List<FindKindData> data) {
		this.data = data;
	}

	public FindAnswer(UnsignedByteBuffer buf) {
		int length = buf.getLengthValue(LIST_LENGTH_FIELD);
		data = new ArrayList<FindKindData>();

		int startPos = buf.position();

		while (buf.getConsumedFrom(startPos) < length) {
			FindKindData d = new FindKindData(buf);
			data.add(d);
		}
	}

	@Override
	protected void implWriteTo(UnsignedByteBuffer buf) {
		Field lenFld = buf.allocateLengthField(LIST_LENGTH_FIELD);

		for (FindKindData d : data) {
			d.writeTo(buf);
		}

		lenFld.setEncodedLength(buf.getConsumedFrom(lenFld.getNextPosition()));
	}

	@Override
	public ContentType getType() {
		return ContentType.FIND_ANS;
	}

	public List<FindKindData> getData() {
		return data;
	}

	@Override
	public String toString() {
		return "FindAnswer [data=" + data + "]";
	}
}
