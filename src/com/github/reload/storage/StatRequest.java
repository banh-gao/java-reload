package com.github.reload.storage;

import java.util.Arrays;
import java.util.List;
import com.github.reload.message.ResourceID;

public class StatRequest extends QueryRequest {

	public StatRequest(Context context, UnsignedByteBuffer buf) throws UnknownKindException {
		super(context, buf);
	}

	public StatRequest(ResourceID id, DataSpecifier... specifiers) {
		this(id, Arrays.asList(specifiers));
	}

	public StatRequest(ResourceID id, List<DataSpecifier> specifiers) {
		super(id, specifiers);
	}

	@Override
	public ContentType getType() {
		return ContentType.STAT_REQ;
	}

}
