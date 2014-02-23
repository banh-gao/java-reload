package com.github.reload.storage.data;

import java.util.HashMap;
import java.util.Map;
import com.github.reload.message.HashAlgorithm;
import com.github.reload.storage.data.DataModel.DataValue;

public abstract class DataModel<T extends DataValue> {

	static {
		registerModel(new SingleModel());
		registerModel(new ArrayModel());
		registerModel(new DictionaryModel());
	}

	private static final Map<String, DataModel<? extends DataValue>> models = new HashMap<String, DataModel<? extends DataValue>>();

	public static DataModel<? extends DataValue> registerModel(DataModel<? extends DataValue> model) {
		return models.put(model.getName(), model);
	}

	public static DataModel<? extends DataValue> getInstance(String name) {
		DataModel<? extends DataValue> model = models.get(name);

		if (model == null)
			throw new IllegalArgumentException("Unhandled data model " + name);

		return model;
	}

	public abstract String getName();

	public abstract DataValueBuilder<T> newValueBuilder();

	public abstract Class<T> getValueClass();

	public abstract Metadata<T> newMetadata(T value, HashAlgorithm hashAlg);

	public abstract Class<? extends Metadata<T>> getMetadataClass();

	public abstract ModelSpecifier<T> newSpecifier();

	public abstract Class<? extends ModelSpecifier<T>> getSpecifierClass();

	/**
	 * The value that will be stored
	 */
	public interface DataValue {

	}

	/**
	 * The metadata associated with a data value
	 */
	public interface Metadata<T extends DataValue> extends DataValue {

	}

	/**
	 * A builder used to create {@link DataValue}
	 * 
	 */
	public interface DataValueBuilder<T> {

		public T build();

	}

	/**
	 * A model specifier used to query for a {@link DataValue}
	 */
	public interface ModelSpecifier<T> {

	}
}
