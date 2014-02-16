package com.github.reload.storage;

import java.math.BigInteger;
import com.github.reload.message.ResourceID;

/**
 * Factory class used to create objects specialized for the single data model
 * 
 * @author Daniel Zozin <zdenial@gmx.com>
 * 
 */
public class SingleModel extends DataModel {

	@Override
	public DataModelSpecifier newSpecifier() {
		return new DataModelSpecifier(ModelType.SINGLE);
	}

	@Override
	public DataModelSpecifier parseSpecifier(UnsignedByteBuffer buf, int length) {
		return new DataModelSpecifier(ModelType.SINGLE);
	}

	@Override
	public DataValue parseValue(UnsignedByteBuffer buf, int length) {
		return new DataValue(buf);
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
	public ModelType getModelType() {
		return ModelType.SINGLE;
	}

	@Override
	protected LocalKindData newLocalKindData(ResourceID resourceId, DataKind dataKind, BigInteger generationCounter, LocalKinds localKinds) {
		return new SingleLocalKindData(resourceId, dataKind, generationCounter, localKinds);
	}
}