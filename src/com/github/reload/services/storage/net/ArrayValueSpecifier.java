package com.github.reload.services.storage.net;

import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.inject.Inject;
import com.github.reload.net.codecs.Codec;
import com.github.reload.net.codecs.Codec.ReloadCodec;
import com.github.reload.services.storage.DataModel.DataValue;
import com.github.reload.services.storage.DataModel.ValueSpecifier;
import com.github.reload.services.storage.net.ArrayValueSpecifier.ArrayValueSpecifierCodec;
import dagger.ObjectGraph;

/**
 * Specifier used to fetch array values
 * 
 */
@ReloadCodec(ArrayValueSpecifierCodec.class)
public class ArrayValueSpecifier implements ValueSpecifier {

	final List<ArrayRange> ranges = new ArrayList<ArrayRange>();

	@Inject
	public ArrayValueSpecifier() {
	}

	/**
	 * Add a range where the returned values must be included, the values
	 * are
	 * 0-indexed
	 * 
	 * @param startIndex
	 *            Start index of the range, included
	 * @param endIndex
	 *            End index of the range, included
	 * @throws IllegalArgumentException
	 *             if the index values are not valid
	 */
	public ArrayValueSpecifier addRange(long startIndex, long endIndex) {
		if (0 <= startIndex && startIndex <= endIndex) {
			ranges.add(new ArrayRange(startIndex, endIndex));
		} else
			throw new IllegalArgumentException("Invalid index values");

		return this;
	}

	/**
	 * @return The array ranges where the returned values must be included
	 */
	public List<ArrayRange> getRanges() {
		return ranges;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ArrayValueSpecifier other = (ArrayValueSpecifier) obj;
		if (ranges == null) {
			if (other.ranges != null)
				return false;
		} else if (!ranges.equals(other.ranges))
			return false;
		return true;
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), ranges);
	}

	public class ArrayRange {

		final long startIndex;
		final long endIndex;

		ArrayRange(long startIndex, long endIndex) {
			this.startIndex = startIndex;
			this.endIndex = endIndex;
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

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			ArrayRange other = (ArrayRange) obj;
			if (endIndex != other.endIndex)
				return false;
			if (startIndex != other.startIndex)
				return false;
			return true;
		}

		@Override
		public int hashCode() {
			return Objects.hash(super.hashCode(), startIndex, endIndex);
		}
	}

	@Override
	public boolean isMatching(DataValue value) {
		if (!(value instanceof ArrayValue))
			return false;

		ArrayValue v = (ArrayValue) value;

		if (!v.getValue().exists())
			return false;

		for (ArrayRange r : getRanges()) {
			if (!r.contains(v.getIndex()))
				return false;
		}

		return true;
	}

	static class ArrayValueSpecifierCodec extends Codec<ArrayValueSpecifier> {

		private static final int RANGES_LENGTH_FIELD = U_INT16;

		public ArrayValueSpecifierCodec(ObjectGraph ctx) {
			super(ctx);
		}

		@Override
		public void encode(ArrayValueSpecifier obj, ByteBuf buf, Object... params) throws CodecException {
			Field lenFld = allocateField(buf, RANGES_LENGTH_FIELD);

			for (ArrayRange r : obj.ranges) {
				buf.writeInt((int) r.startIndex);
				buf.writeInt((int) r.endIndex);
			}

			lenFld.updateDataLength();
		}

		@Override
		public ArrayValueSpecifier decode(ByteBuf buf, Object... params) throws CodecException {

			ByteBuf rangesData = readField(buf, RANGES_LENGTH_FIELD);

			ArrayValueSpecifier spec = new ArrayValueSpecifier();

			while (rangesData.readableBytes() > 0) {
				long startIndex = rangesData.readUnsignedInt();
				long endIndex = rangesData.readUnsignedInt();
				spec.addRange(startIndex, endIndex);
			}

			rangesData.release();

			return spec;
		}
	}
}