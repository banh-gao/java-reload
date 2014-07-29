package com.github.reload.storage.encoders;

import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import com.github.reload.conf.Configuration;
import com.github.reload.net.encoders.Codec;
import com.github.reload.net.encoders.Codec.ReloadCodec;
import com.github.reload.net.encoders.secBlock.HashAlgorithm;
import com.github.reload.storage.DataModel;
import com.github.reload.storage.DataModel.ModelName;
import com.github.reload.storage.PreparedData.DataBuildingException;
import com.github.reload.storage.encoders.ArrayModel.ArrayModelSpecifier.ArrayRange;

/**
 * Factory class used to create objects specialized for the array data model
 * 
 */
@ModelName("ARRAY")
public class ArrayModel extends DataModel<ArrayValue> {

	@Override
	public Class<ArrayValue> getValueClass() {
		return ArrayValue.class;
	}

	@Override
	public ArrayValueBuilder newValueBuilder() {
		return new ArrayValueBuilder();
	}

	@Override
	public ArrayMetadata newMetadata(ArrayValue value, HashAlgorithm hashAlg) {
		SingleModel singleModel = (SingleModel) getInstance(DataModel.SINGLE);
		SingleMetadata singleMeta = singleModel.newMetadata(value.getValue(), hashAlg);
		return new ArrayMetadata(value.getIndex(), singleMeta);
	}

	@Override
	public Class<ArrayMetadata> getMetadataClass() {
		return ArrayMetadata.class;
	}

	@Override
	public ArrayModelSpecifier newSpecifier() {
		return new ArrayModelSpecifier();
	}

	@Override
	public Class<ArrayModelSpecifier> getSpecifierClass() {
		return ArrayModelSpecifier.class;
	}

	/**
	 * An array prepared value created by adding an index to a single prepared
	 * value
	 * 
	 * @author Daniel Zozin <zdenial@gmx.com>
	 * 
	 */
	public class ArrayValueBuilder implements DataValueBuilder<ArrayValue> {

		/**
		 * Indicates the last index position in an array, used to append
		 * elements to
		 * the array
		 */
		public static final long LAST_INDEX = 0xffffffffl;

		private long index = -1;
		private boolean append = false;
		private SingleValue value;

		public ArrayValueBuilder index(long index) {
			this.index = index;
			return this;
		}

		public ArrayValueBuilder append(boolean append) {
			this.append = append;
			return this;
		}

		public ArrayValueBuilder value(SingleValue value) {
			this.value = value;
			return this;
		}

		@Override
		public ArrayValue build() {
			if (append)
				index = LAST_INDEX;

			if (index < 0)
				throw new DataBuildingException("Array index not set");

			return new ArrayValue(index, value);
		}
	}

	/**
	 * Specifier used to fetch array values
	 * 
	 */
	@ReloadCodec(ArrayModelSpecifierCodec.class)
	public static class ArrayModelSpecifier implements ModelSpecifier<ArrayValue> {

		private final List<ArrayRange> ranges = new ArrayList<ArrayRange>();

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

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			ArrayModelSpecifier other = (ArrayModelSpecifier) obj;
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

		class ArrayRange {

			private final long startIndex;
			private final long endIndex;

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
		public boolean isMatching(ArrayValue value) {
			for (ArrayRange r : getRanges()) {
				if (r.contains(value.getIndex()))
					return true;
			}
			return false;
		}
	}

	static class ArrayModelSpecifierCodec extends Codec<ArrayModelSpecifier> {

		private static final int RANGES_LENGTH_FIELD = U_INT16;

		public ArrayModelSpecifierCodec(Configuration conf) {
			super(conf);
		}

		@Override
		public void encode(ArrayModelSpecifier obj, ByteBuf buf, Object... params) throws CodecException {
			Field lenFld = allocateField(buf, RANGES_LENGTH_FIELD);

			for (ArrayRange r : obj.ranges) {
				buf.writeInt((int) r.startIndex);
				buf.writeInt((int) r.endIndex);
			}

			lenFld.updateDataLength();
		}

		@Override
		public ArrayModelSpecifier decode(ByteBuf buf, Object... params) throws CodecException {

			ByteBuf rangesData = readField(buf, RANGES_LENGTH_FIELD);

			ArrayModelSpecifier spec = new ArrayModelSpecifier();

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