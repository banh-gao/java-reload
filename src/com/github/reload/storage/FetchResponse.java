package com.github.reload.storage;

import java.math.BigInteger;
import java.util.List;

/**
 * A response contained in a fetch answer, contains all the data for a specific
 * kind that matches the request data specifier
 * 
 * @author Daniel Zozin <zdenial@gmx.com>
 * 
 */
public class FetchResponse extends DataResponse<StoredData> {

	public FetchResponse(DataKind kind, BigInteger generation, List<StoredData> values) {
		super(kind, generation, values);
	}

	public FetchResponse(Context context, UnsignedByteBuffer buf) throws UnknownKindException {
		super(context, buf);
	}

	@Override
	protected StoredData decodeData(DataKind kind, UnsignedByteBuffer buf) {
		return new StoredData(kind, buf);
	}
}
