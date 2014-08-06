package com.github.reload.services.storage.encoders;

import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import com.github.reload.components.ComponentsContext;
import com.github.reload.net.encoders.Codec;
import com.github.reload.net.encoders.Codec.ReloadCodec;
import com.github.reload.net.encoders.secBlock.HashAlgorithm;
import com.github.reload.services.storage.encoders.ArrayModel.ArrayValueSpecifier.ArrayRange;
import com.github.reload.services.storage.encoders.DataModel.ModelName;

/**
 * Factory class used to create objects specialized for the array data model
 * 
 */
@ModelName("ARRAY")
public class ArrayModel extends DataModel<ArrayValue> {

	private static final ArrayValue NON_EXISTENT = new ArrayValue(0, new SingleValue(new byte[0], false));

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
		SingleModel singleModel = getInstance(DataModel.SINGLE);
		SingleMetadata singleMeta = singleModel.newMetadata(value.getValue(), hashAlg);
		return new ArrayMetadata(value.getIndex(), singleMeta);
	}

	@Override
	public Class<ArrayMetadata> getMetadataClass() {
		return ArrayMetadata.class;
	}

	@Override
	public ArrayValueSpecifier newSpecifier() {
		return new ArrayValueSpecifier();
	}

	@Override
	public Class<ArrayValueSpecifier> getSpecifierClass() {
		return ArrayValueSpecifier.class;
	}

	@Override
	public ArrayValue getNonExistentValue() {
		return NON_EXISTENT;
	}

	/**
	 * An array prepared value created by adding an index to a single prepared
	 * value
	 * 
	 */
	public class ArrayValueBuilder implements DataValueBuilder<ArrayValue> {

		/**
		 * Indicates the last index position in an array, used to append
		 * elements to
		 * the array
		 */
		public static final long LAST_INDEX = 0xffffffffl;

		private final SingleValue DEFAULT_VALUE = new SingleValue(new byte[0], false);

		private long index = 0;
		private boolean append = false;
		private SingleValue value = DEFAULT_VALUE;

		public ArrayValueBuilder index(long index) {
			this.index = index;
			return this;
		}

		public ArrayValueBuilder append(boolean append) {
			this.append = append;
			return this;
		}

		public ArrayValueBuilder value(byte[] value, boolean exists) {
			this.value = new SingleValue(value, exists);
			return this;
		}

		@Override
		public ArrayValue build() {
			if (append) {
				index = LAST_INDEX;
			}

			return new ArrayValue(index, value);
		}
	}

	/**
	 * Specifier used to fetch array values
	 * 
	 */
	@ReloadCodec(ArrayValueSpecifierCodec.class)
	public static class ArrayValueSpecifier implements ValueSpecifier {

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
	}

	static class ArrayValueSpecifierCodec extends Codec<ArrayValueSpecifier> {

		private static final int RANGES_LENGTH_FIELD = U_INT16;

		public ArrayValueSpecifierCodec(ComponentsContext ctx) {
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