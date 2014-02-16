package com.github.reload.storage;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public abstract class DataResponse<T extends DataResponse.ResponseData> implements Iterable<T> {

	private final static int VALUES_LENGTH_FIELD = EncUtils.U_INT32;

	private final DataKind kind;
	private final BigInteger generation;
	private final List<T> values;

	public DataResponse(DataKind kind, BigInteger generation, List<T> values) {
		this.kind = kind;
		this.generation = generation;
		this.values = values;
	}

	public DataResponse(Context context, UnsignedByteBuffer buf) throws UnknownKindException {
		KindId id = KindId.valueOf(buf);
		kind = context.getConfiguration().getDataKind(id);
		if (kind == null)
			throw new UnknownKindException(id);

		generation = buf.getSigned64();
		values = decodedValues(kind, buf);
	}

	private List<T> decodedValues(DataKind kind, UnsignedByteBuffer buf) {
		int length = buf.getLengthValue(VALUES_LENGTH_FIELD);

		List<T> out = new ArrayList<T>();

		int startPos = buf.position();

		while (buf.getConsumedFrom(startPos) < length) {
			T data = decodeData(kind, buf);
			out.add(data);
		}
		return out;
	}

	protected abstract T decodeData(DataKind kind, UnsignedByteBuffer buf);

	public BigInteger getGenerationCounter() {
		return generation;
	}

	public DataKind getKind() {
		return kind;
	}

	public List<T> getValues() {
		return values;
	}

	public void writeTo(UnsignedByteBuffer buf) {
		kind.getKindId().writeTo(buf);
		buf.putUnsigned64(generation);

		Field lenFld = buf.allocateLengthField(VALUES_LENGTH_FIELD);

		for (T d : values) {
			d.writeTo(buf);
		}

		lenFld.setEncodedLength(buf.getConsumedFrom(lenFld.getNextPosition()));
	}

	@Override
	public Iterator<T> iterator() {
		return values.iterator();
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + " [kind=" + kind.getKindId() + ", generation=" + generation + ", values=" + values + "]";
	}

	/**
	 * An answer data to a storage query
	 * 
	 * @author Daniel Zozin <zdenial@gmx.com>
	 * 
	 */
	public static abstract class ResponseData {

		protected abstract void writeTo(UnsignedByteBuffer buf);

		protected abstract SignerIdentity getSignerIdentity();
	}
}
