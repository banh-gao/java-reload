package com.github.reload.storage.data;

import java.util.HashMap;
import java.util.Map;
import com.github.reload.net.encoders.content.storage.ArrayModel;
import com.github.reload.net.encoders.content.storage.DictionaryModel;
import com.github.reload.net.encoders.content.storage.SingleModel;
import com.github.reload.net.encoders.secBlock.HashAlgorithm;
import com.github.reload.storage.data.DataModel.DataValue;

public abstract class DataModel<T extends DataValue> {

	static {
		try {
			registerModel(SingleModel.class);
			registerModel(ArrayModel.class);
			registerModel(DictionaryModel.class);
		} catch (InstantiationException | IllegalAccessException e) {
			e.printStackTrace();
		}
	}

	private static final Map<String, DataModel<? extends DataValue>> models = new HashMap<String, DataModel<? extends DataValue>>();

	public static <T extends DataValue> void registerModel(Class<? extends DataModel<T>> modelClazz) throws InstantiationException, IllegalAccessException {
		DataModel<T> model = modelClazz.newInstance();
		models.put(model.getName(), model);
	}

	public static DataModel<? extends DataValue> getInstance(String name) {
		DataModel<? extends DataValue> model = models.get(name);

		if (model == null)
			throw new IllegalArgumentException("No data model for type " + name);

		return model;
	}

	public DataModel() {
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
