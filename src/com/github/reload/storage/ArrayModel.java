package com.github.reload.storage;

import java.math.BigInteger;
import com.github.reload.message.ResourceID;

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
	public ArrayValue parseValue(UnsignedByteBuffer buf, int length) {
		return new ArrayValue(buf);
	}

	@Override
	public ArrayPreparedValue newPreparedValue(DataKind dataKind) {
		return new ArrayPreparedValue(dataKind);
	}

	/**
	 * Get array with index set to 0 to avoid signature breaking when using
	 * the append feature (index to 0xffffffff)
	 */
	public static ArrayValue getValueForSigning(ArrayValue currentValue, DataKind dataKind) {
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
	public ModelType getModelType() {
		return ModelType.ARRAY;
	}

	@Override
	protected LocalKindData newLocalKindData(ResourceID resourceId, DataKind dataKind, BigInteger generationCounter, LocalKinds localKinds) {
		return new ArrayLocalKindData(resourceId, dataKind, generationCounter, localKinds);
	}
}