package com.github.reload.storage;

import java.math.BigInteger;
import com.github.reload.message.ResourceID;
import com.github.reload.storage.data.ArrayMetadata;
import com.github.reload.storage.data.ArrayEntry;

/**
 * Factory class used to create objects specialized for the array data model
 * 
 * @author Daniel Zozin <zdenial@gmx.com>
 * 
 */
public class ArrayModel extends DataModel {

	/**
	 * Indicates the last index position in an array, used to append elements to
	 * the array
	 */
	public static final int LAST_INDEX = EncUtils.U_INT32;

	@Override
	public ArrayModelSpecifier newSpecifier() {
		return new ArrayModelSpecifier();
	}

	@Override
	public ArrayModelSpecifier parseSpecifier(UnsignedByteBuffer buf, int length) {
		return new ArrayModelSpecifier(buf);
	}

	@Override
	public ArrayEntry parseValue(UnsignedByteBuffer buf, int length) {
		return new ArrayEntry(buf);
	}

	@Override
	public ArrayPreparedValue newPreparedValue(DataKind dataKind) {
		return new ArrayPreparedValue(dataKind);
	}

	/**
	 * Get array with index set to 0 to avoid signature breaking when using
	 * the append feature (index to 0xffffffff)
	 */
	public static ArrayEntry getValueForSigning(ArrayEntry currentValue, DataKind dataKind) {
		ArrayPreparedValue p = new ArrayPreparedValue(dataKind);
		p.setIndex(0);
		p.setExists(currentValue.exists());
		p.setValue(currentValue.getValue());
		return p.build();
	}

	@Override
	public Metadata parseMetadata(UnsignedByteBuffer buf, int length) {
		return new ArrayMetadata(buf);
	}

	@Override
	public DataType getModelType() {
		return DataType.ARRAY;
	}

	@Override
	protected LocalKindData newLocalKindData(ResourceID resourceId, DataKind dataKind, BigInteger generationCounter, LocalKinds localKinds) {
		return new ArrayLocalKindData(resourceId, dataKind, generationCounter, localKinds);
	}
}