package com.github.reload.storage;

import java.util.ArrayList;
import java.util.List;
import com.github.reload.message.ResourceID;

public class StoreRequest extends MessageContent {

	private static final int STOREDKINDDATA_LENGTH_FIELD = EncUtils.U_INT32;

	private final ResourceID resourceId;
	private final short replicaNumber;
	private final List<StoredKindData> kindData;

	public StoreRequest(ResourceID id, short replicaNumber, List<StoredKindData> data) {
		resourceId = id;
		this.replicaNumber = replicaNumber;
		kindData = data;
	}

	public StoreRequest(Context context, UnsignedByteBuffer buf) throws StorageException {
		resourceId = ResourceID.valueOf(buf);
		replicaNumber = buf.getSigned8();
		kindData = decodeKindDataList(context, buf);
	}

	@Override
	protected void implWriteTo(UnsignedByteBuffer buf) {
		resourceId.writeTo(buf);
		buf.putUnsigned8(replicaNumber);

		Field lenFld = buf.allocateLengthField(STOREDKINDDATA_LENGTH_FIELD);

		for (StoredKindData d : kindData) {
			d.writeTo(buf);
		}

		lenFld.setEncodedLength(buf.getConsumedFrom(lenFld.getNextPosition()));
	}

	@Override
	public ContentType getType() {
		return ContentType.STORE_REQ;
	}

	private static List<StoredKindData> decodeKindDataList(Context context, UnsignedByteBuffer buf) throws StorageException {
		int length = buf.getLengthValue(StoreRequest.STOREDKINDDATA_LENGTH_FIELD);
		List<StoredKindData> out = new ArrayList<StoredKindData>();

		int startPos = buf.position();

		while (buf.getConsumedFrom(startPos) < length) {
			StoredKindData data = new StoredKindData(context, buf);
			out.add(data);
		}
		return out;
	}

	public ResourceID getResourceId() {
		return resourceId;
	}

	public List<StoredKindData> getKindData() {
		return kindData;
	}

	public short getReplicaNumber() {
		return replicaNumber;
	}
}
