package com.github.reload.storage;

import java.math.BigInteger;
import com.github.reload.message.ResourceID;
import com.github.reload.storage.data.DictionaryMetadata;
import com.github.reload.storage.data.DictionaryValue;

/**
 * Factory class used to create objects specialized for the dictionary data
 * model
 * 
 * @author Daniel Zozin <zdenial@gmx.com>
 * 
 */
public class DictionaryModel extends DataModel {

	@Override
	public DictionaryModelSpecifier newSpecifier() {
		return new DictionaryModelSpecifier();
	}

	@Override
	public DictionaryModelSpecifier parseSpecifier(UnsignedByteBuffer buf, int length) {
		return new DictionaryModelSpecifier(buf);
	}

	@Override
	public DictionaryValue parseValue(UnsignedByteBuffer buf, int length) {
		return new DictionaryValue(buf);
	}

	@Override
	public DictionaryPreparedValue newPreparedValue(DataKind dataKind) {
		return new DictionaryPreparedValue(dataKind);
	}

	@Override
	public Metadata parseMetadata(UnsignedByteBuffer buf, int length) {
		return new DictionaryMetadata(buf);
	}

	@Override
	public ModelType getModelType() {
		return ModelType.DICTIONARY;
	}

	@Override
	protected LocalKindData newLocalKindData(ResourceID resourceId, DataKind dataKind, BigInteger generationCounter, LocalKinds localKinds) {
		return new DictionaryLocalKindData(resourceId, dataKind, generationCounter, localKinds);
	}
}