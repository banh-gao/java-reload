package com.github.reload.storage;

import java.math.BigInteger;
import java.util.EnumSet;
import com.github.reload.message.ResourceID;
import com.github.reload.storage.data.ArrayEntry;
import com.github.reload.storage.data.SingleEntry;
import com.github.reload.storage.data.DictionaryEntry;

public abstract class DataModel {

	public enum DataType {
		SINGLE("SINGLE", SingleEntry.class),
		ARRAY("ARRAY", ArrayEntry.class),
		DICTIONARY("DICTIONARY", DictionaryEntry.class);

		final String name;
		final Class<? extends SingleEntry> valueClass;

		private DataType(String name, Class<? extends SingleEntry> valueClass) {
			this.name = name;
			this.valueClass = valueClass;
		}

		public static DataType fromString(String v) {
			for (DataType t : EnumSet.allOf(DataType.class))
				if (t.name.equalsIgnoreCase(v))
					return t;
			return null;
		}

		public String getName() {
			return name;
		}

		public Class<? extends SingleEntry> getValueClass() {
			return valueClass;
		}

		@Override
		public String toString() {
			return getName();
		}
	}

	private static final SingleModel SINGLE_INSTANCE = new SingleModel();
	private static final ArrayModel ARRAY_INSTANCE = new ArrayModel();
	private static final DictionaryModel DICT_INSTANCE = new DictionaryModel();

	public static DataModel getInstance(DataType mod) {
		switch (mod) {
			case SINGLE :
				return SINGLE_INSTANCE;
			case ARRAY :
				return ARRAY_INSTANCE;
			case DICTIONARY :
				return DICT_INSTANCE;
		}
		throw new IllegalArgumentException("Unhandled value " + mod);
	}

	public static <B extends PreparedValue, S extends DataModelSpecifier> DataModel getInstance(String model) {
		return getInstance(DataType.fromString(model));
	}

	public abstract DataModelSpecifier newSpecifier();

	protected abstract LocalKindData newLocalKindData(ResourceID resourceId, DataKind dataKind, BigInteger generationCounter, LocalKinds localKinds);

	public abstract PreparedValue newPreparedValue(DataKind dataKind);

	public abstract DataType getDataType();

	@Override
	public String toString() {
		return getDataType().getName();
	}
}
