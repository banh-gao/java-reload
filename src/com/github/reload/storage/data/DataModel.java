package com.github.reload.storage.data;

import java.util.HashMap;
import java.util.Map;

public abstract class DataModel {

	static {
		registerModel(new SingleModel());
		registerModel(new ArrayModel());
		registerModel(new DictionaryModel());
	}

	private static final Map<String, DataModel> models = new HashMap<String, DataModel>();

	public static DataModel registerModel(DataModel model) {
		return models.put(model.getName(), model);
	}

	public static DataModel getInstance(String name) {
		DataModel model = models.get(name);

		if (model == null)
			throw new IllegalArgumentException("Unhandled data model " + name);

		return model;
	}

	public abstract String getName();

	public abstract DataValueBuilder newValueBuilder();

	public abstract Class<? extends ModelSpecifier> getSpecifierClass();

	public abstract ModelSpecifier newSpecifier();

	public interface DataValue {

	}

	/**
	 * A builder used to create values for a data model
	 * 
	 */
	public interface DataValueBuilder {

		public DataValue build();

	}

	/**
	 * A model specifier used to fetch data of a particular data model
	 */
	public interface ModelSpecifier {

	}

}
