package com.github.reload.services.storage;

import com.github.reload.net.encoders.secBlock.HashAlgorithm;
import com.github.reload.services.storage.net.ArrayMetadata;
import com.github.reload.services.storage.net.ArrayValue;
import com.github.reload.services.storage.net.ArrayValueSpecifier;
import com.github.reload.services.storage.net.DictionaryMetadata;
import com.github.reload.services.storage.net.DictionaryValue;
import com.github.reload.services.storage.net.DictionaryValueSpecifier;
import com.github.reload.services.storage.net.SingleMetadata;
import com.github.reload.services.storage.net.SingleValue;
import com.github.reload.services.storage.net.SingleValueSpecifier;

public enum DataModel {

	SINGLE("SINGLE", SingleValue.class, SingleValueSpecifier.class, SingleMetadata.class, new SingleValue(new byte[0], false)),
	ARRAY("ARRAY", ArrayValue.class, ArrayValueSpecifier.class, ArrayMetadata.class, new ArrayValue(0, new SingleValue(new byte[0], false))),
	DICTIONARY("DICT", DictionaryValue.class, DictionaryValueSpecifier.class, DictionaryMetadata.class, new DictionaryValue(new byte[0], new SingleValue(new byte[0], false)));

	final String name;

	// DATA VALUE TYPE (Single, Array, Dictionary)
	final Class<? extends DataValue> valueClass;

	// DATA SPECIFIER TYPE
	final Class<? extends ValueSpecifier> specifierClass;

	// METADATA TYPE
	final Class<? extends Metadata> metadataClass;

	/**
	 * Syntetic data value used to indicate non existent data in particular
	 * situations
	 */
	final DataValue nonExistentValue;

	private DataModel(String name, Class<? extends DataValue> valueClass, Class<? extends ValueSpecifier> specifierClass, Class<? extends Metadata> metadataClass, DataValue nonExistentValue) {
		this.name = name;
		this.valueClass = valueClass;
		this.specifierClass = specifierClass;
		this.metadataClass = metadataClass;
		this.nonExistentValue = nonExistentValue;
	}

	public String getName() {
		return name;
	}

	public Class<? extends DataValue> getValueClass() {
		return valueClass;
	}

	public Class<? extends ValueSpecifier> getSpecifierClass() {
		return specifierClass;
	}

	public Class<? extends Metadata> getMetadataClass() {
		return metadataClass;
	}

	public DataValue getNonExistentValue() {
		return nonExistentValue;
	}

	/**
	 * The value that will be stored
	 */
	public interface DataValue {

		public ValueSpecifier getMatchingSpecifier();

		long getSize();

	}

	/**
	 * The metadata associated with a data value
	 */
	public interface Metadata extends DataValue {

		public void setMetadata(DataValue value, HashAlgorithm hashAlg);
	}

	/**
	 * A model specifier used to query for a {@link DataValue}
	 */
	public interface ValueSpecifier {

		boolean isMatching(DataValue value);

	}
}
