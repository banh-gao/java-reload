package com.github.reload.storage;

import java.util.Arrays;
import java.util.List;
import com.github.reload.message.ResourceID;

/**
 * A fetch request for overlay storage
 * 
 * @author Daniel Zozin <zdenial@gmx.com>
 * 
 */
public class FetchRequest extends QueryRequest {

	public FetchRequest(Context context, UnsignedByteBuffer buf) throws UnknownKindException {
		super(context, buf);
	}

	public FetchRequest(ResourceID id, DataSpecifier... specifiers) {
		this(id, Arrays.asList(specifiers));
	}

	public FetchRequest(ResourceID id, List<DataSpecifier> specifiers) {
		super(id, specifiers);
	}

	@Override
	public ContentType getType() {
		return ContentType.FETCH_REQ;
	}

}
