package com.github.reload.storage;

import java.math.BigInteger;
import com.github.reload.message.ResourceID;
import com.github.reload.storage.data.SingleEntry;

/**
 * Factory class used to create objects specialized for the single data model
 * 
 * @author Daniel Zozin <zdenial@gmx.com>
 * 
 */
public class SingleModel extends DataModel {

	@Override
	public DataModelSpecifier newSpecifier() {
		return new DataModelSpecifier(DataType.SINGLE);
	}

	@Override
	public DataModelSpecifier parseSpecifier(UnsignedByteBuffer buf, int length) {
		return new DataModelSpecifier(DataType.SINGLE);
	}

	@Override
	public SingleEntry parseValue(UnsignedByteBuffer buf, int length) {
		return new SingleEntry(buf);
	}

	@Override
	public PreparedValue newPreparedValue(DataKind dataKind) {
		return new PreparedValue(dataKind);
	}

	@Override
	public Metadata parseMetadata(UnsignedByteBuffer buf, int length) {
		return new Metadata(buf);
	}

	@Override
	public DataType getModelType() {
		return DataType.SINGLE;
	}

	@Override
	protected LocalKindData newLocalKindData(ResourceID resourceId, DataKind dataKind, BigInteger generationCounter, LocalKinds localKinds) {
		return new SingleLocalKindData(resourceId, dataKind, generationCounter, localKinds);
	}
}