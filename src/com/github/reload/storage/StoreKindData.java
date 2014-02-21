package com.github.reload.storage;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import com.github.reload.storage.data.StoredData;

/**
 * Used in store and fetch to represent values for a specific data kind
 * 
 * @author Daniel Zozin <zdenial@gmx.com>
 * 
 */
public class StoreKindData {

	protected static final int VALUES_LENGTH_FIELD = EncUtils.U_INT32;

	protected final DataKind kind;

	private BigInteger generationCounter;

	private final List<StoredData> data = new ArrayList<StoredData>();

	public StoreKindData(DataKind kind, BigInteger generationCounter) {
		this.kind = kind;
		this.generationCounter = generationCounter;
	}

	public StoreKindData(Context context, UnsignedByteBuffer buf) throws UnknownKindException {
		KindId id = KindId.valueOf(buf);
		kind = context.getConfiguration().getDataKind(id);
		if (kind == null)
			throw new UnknownKindException(id);

		generationCounter = buf.getSigned64();

		decodeStoredDataList(kind, buf);
	}

	private void decodeStoredDataList(DataKind kind, UnsignedByteBuffer buf) {
		int length = buf.getLengthValue(StoreKindData.VALUES_LENGTH_FIELD);

		int startPos = buf.position();

		while (buf.getConsumedFrom(startPos) < length) {
			StoredData data = new StoredData(kind, buf);
			this.data.add(data);
		}
	}

	/**
	 * Add a new data to the kind by authenticating the data and checking the
	 * validity of the request
	 * 
	 * @throws DataTooOldException
	 * @throws ForbittenException
	 */
	public void add(StoredData requestData) {
		data.add(requestData);
	}

	public DataKind getKind() {
		return kind;
	}

	public BigInteger getGeneration() {
		return generationCounter;
	}

	void setGeneration(BigInteger generation) {
		generationCounter = generation;
	}

	public void writeTo(UnsignedByteBuffer buf) {
		kind.getKindId().writeTo(buf);
		buf.putUnsigned64(generationCounter);

		Field lenFld = buf.allocateLengthField(VALUES_LENGTH_FIELD);

		for (StoredData d : data) {
			d.writeTo(buf);
		}

		lenFld.setEncodedLength(buf.getConsumedFrom(lenFld.getNextPosition()));
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((kind == null) ? 0 : kind.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		StoreKindData other = (StoreKindData) obj;
		if (kind == null) {
			if (other.kind != null)
				return false;
		} else if (!kind.equals(other.kind))
			return false;
		return true;
	}

	public List<StoredData> getValues() {
		return data;
	}

	public int size() {
		return data.size();
	}
}
