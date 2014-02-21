package com.github.reload.storage;

import java.util.ArrayList;
import java.util.List;
import com.github.reload.storage.DataModel.DataType;

/**
 * Specifier used to fetch array values
 * 
 * @author Daniel Zozin <zdenial@gmx.com>
 * 
 */
public class ArrayModelSpecifier extends DataModelSpecifier {

	private static final int RANGES_LENGTH_FIELD = EncUtils.U_INT16;
	private static final int ARRAYRANGE_LENGTH = EncUtils.U_INT32 + EncUtils.U_INT32;

	private final List<ArrayRange> ranges = new ArrayList<ArrayRange>();

	ArrayModelSpecifier() {
		super(DataType.ARRAY);
	}

	ArrayModelSpecifier(UnsignedByteBuffer buf) {
		super(DataType.ARRAY);
		int length = buf.getLengthValue(ArrayModelSpecifier.RANGES_LENGTH_FIELD);

		while (length > 0) {
			ArrayRange range = new ArrayRange(buf);
			ranges.add(range);
			length -= ArrayModelSpecifier.ARRAYRANGE_LENGTH;
		}
	}

	/**
	 * Add a range where the returned values must be included, the values are
	 * 0-indexed
	 * 
	 * @param startIndex
	 *            Start index of the range, included
	 * @param endIndex
	 *            End index of the range, included
	 * @throws IllegalArgumentException
	 *             if the index values are not valid
	 */
	public void addRange(long startIndex, long endIndex) {
		if (0 <= startIndex && startIndex <= endIndex) {
			ranges.add(new ArrayRange(startIndex, endIndex));
		} else
			throw new IllegalArgumentException("Invalid index values");
	}

	/**
	 * @return The array ranges where the returned values must be included
	 */
	List<ArrayRange> getRanges() {
		return ranges;
	}

	class ArrayRange {

		private final long startIndex;
		private final long endIndex;

		ArrayRange(long startIndex, long endIndex) {
			this.startIndex = startIndex;
			this.endIndex = endIndex;
		}

		ArrayRange(UnsignedByteBuffer buf) {
			startIndex = buf.getSigned32();
			endIndex = buf.getSigned32();
		}

		public long getStartIndex() {
			return startIndex;
		}

		public long getEndIndex() {
			return endIndex;
		}

		public boolean contains(long index) {
			return startIndex <= index && index <= endIndex;
		}

		public void writeTo(UnsignedByteBuffer buf) {
			buf.putUnsigned32(startIndex);
			buf.putUnsigned32(endIndex);
		}
	}

	@Override
	void writeTo(UnsignedByteBuffer buf) {

		Field lenFld = buf.allocateLengthField(RANGES_LENGTH_FIELD);

		for (ArrayRange r : ranges) {
			r.writeTo(buf);
		}

		lenFld.setEncodedLength(buf.getConsumedFrom(lenFld.getNextPosition()));
	}
}