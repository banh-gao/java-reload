package com.github.reload.storage;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import com.github.reload.message.ResourceID;
import com.github.reload.message.errors.InvalidMessageException;

public class FindRequest extends MessageContent {

	private static final int KINDS_LENGTH_FIELD = EncUtils.U_INT8;

	private final ResourceID resourceId;
	private final Set<KindId> kinds;

	public FindRequest(ResourceID resourceId, DataKind... kinds) {
		this(resourceId, Arrays.asList(kinds));
	}

	public FindRequest(ResourceID resourceId, List<DataKind> kinds) {
		this.resourceId = resourceId;
		this.kinds = new LinkedHashSet<KindId>();
		for (DataKind k : kinds) {
			if (!this.kinds.add(k.getKindId()))
				throw new IllegalArgumentException("Duplicated data kind");
		}
	}

	public FindRequest(UnsignedByteBuffer buf) throws InvalidMessageException {
		resourceId = ResourceID.valueOf(buf);

		int length = buf.getLengthValue(KINDS_LENGTH_FIELD);
		kinds = new LinkedHashSet<KindId>();

		int startPos = buf.position();

		while (buf.getConsumedFrom(startPos) < length) {
			KindId kindId = KindId.valueOf(buf);
			if (!kinds.add(kindId))
				throw new InvalidMessageException("Duplicate kind-id " + kindId.getId());
		}
	}

	@Override
	protected void implWriteTo(UnsignedByteBuffer buf) {
		resourceId.writeTo(buf);
		Field lenFld = buf.allocateLengthField(KINDS_LENGTH_FIELD);

		for (KindId k : kinds) {
			k.writeTo(buf);
		}

		lenFld.setEncodedLength(buf.getConsumedFrom(lenFld.getNextPosition()));
	}

	@Override
	public ContentType getType() {
		return ContentType.FIND_REQ;
	}

	public ResourceID getResourceId() {
		return resourceId;
	}

	public Set<KindId> getKinds() {
		return kinds;
	}
}
