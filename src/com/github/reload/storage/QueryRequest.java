package com.github.reload.storage;

import java.util.ArrayList;
import java.util.List;
import com.github.reload.message.ResourceID;

/**
 * A storage query request for resources (includes fetch and stat request)
 * 
 * @author Daniel Zozin <zdenial@gmx.com>
 * 
 */
public abstract class QueryRequest extends MessageContent {

	private static final int SPECIFIERS_LENGTH_FIELD = EncUtils.U_INT16;

	private final ResourceID resourceId;
	private final List<DataSpecifier> specifiers;

	public QueryRequest(ResourceID id, List<DataSpecifier> specifiers) {
		resourceId = id;
		this.specifiers = specifiers;
	}

	public QueryRequest(Context context, UnsignedByteBuffer buf) throws UnknownKindException {
		resourceId = ResourceID.valueOf(buf);
		specifiers = readSpecifiers(context, buf);
	}

	private static List<DataSpecifier> readSpecifiers(Context context, UnsignedByteBuffer buf) throws UnknownKindException {
		int length = buf.getLengthValue(QueryRequest.SPECIFIERS_LENGTH_FIELD);

		List<DataSpecifier> out = new ArrayList<DataSpecifier>();

		int startPos = buf.position();

		while (buf.getConsumedFrom(startPos) < length) {
			DataSpecifier spec = new DataSpecifier(context.getConfiguration(), buf);
			out.add(spec);
		}
		return out;
	}

	public ResourceID getResourceId() {
		return resourceId;
	}

	public List<DataSpecifier> getDataSpecifiers() {
		return specifiers;
	}

	@Override
	protected void implWriteTo(UnsignedByteBuffer buf) {
		resourceId.writeTo(buf);
		writeSpecifiersTo(buf);
	}

	private void writeSpecifiersTo(UnsignedByteBuffer buf) {
		Field lenFld = buf.allocateLengthField(SPECIFIERS_LENGTH_FIELD);

		for (DataSpecifier s : specifiers) {
			s.writeTo(buf);
		}

		lenFld.setEncodedLength(buf.getConsumedFrom(lenFld.getNextPosition()));
	}
}
