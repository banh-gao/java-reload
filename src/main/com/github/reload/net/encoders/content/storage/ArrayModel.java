package com.github.reload.net.encoders.content.storage;

import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import com.github.reload.Configuration;
import com.github.reload.net.encoders.Codec;
import com.github.reload.net.encoders.Codec.ReloadCodec;
import com.github.reload.net.encoders.content.storage.ArrayModel.ArrayModelSpecifier.ArrayRange;
import com.github.reload.net.encoders.secBlock.HashAlgorithm;
import com.github.reload.storage.DataModel;
import com.github.reload.storage.DataModel.ModelName;
import com.github.reload.storage.PreparedData.DataBuildingException;

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
	public DataValueBuilder<ArrayValue> newValueBuilder() {
		return new ArrayValueBuilder();
	}

	@Override
	public Metadata<ArrayValue> newMetadata(ArrayValue value, HashAlgorithm hashAlg) {
		SingleModel singleModel = (SingleModel) getInstance(DataModel.SINGLE);
		SingleMetadata singleMeta = singleModel.newMetadata(value.getValue(), hashAlg);
		return new ArrayMetadata(value.getIndex(), singleMeta);
	}

	@Override
	public Class<? extends Metadata<ArrayValue>> getMetadataClass() {
		return ArrayMetadata.class;
	}

	@Override
	public ModelSpecifier<ArrayValue> newSpecifier() {
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
		public static final int LAST_INDEX = 0xffffffff;

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

		public ArrayValueBuilder setValue(SingleValue value) {
			this.value = value;
			return this;
		}

		@Override
		public ArrayValue build() {
			if (index < 0)
				throw new DataBuildingException("Array index not set");

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
		}
	}

	public static class ArrayModelSpecifierCodec extends Codec<ArrayModelSpecifier> {

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
				long startIndex = buf.readUnsignedInt();
				long endIndex = buf.readUnsignedInt();
				spec.addRange(startIndex, endIndex);
			}

			return spec;
		}
	}
}