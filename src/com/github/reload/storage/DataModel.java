package com.github.reload.storage;

import java.math.BigInteger;
import java.util.EnumSet;
import com.github.reload.message.ResourceID;

public abstract class DataModel {

	public enum ModelType {
		SINGLE("SINGLE"), ARRAY("ARRAY"), DICTIONARY("DICTIONARY");

		final String name;

		private ModelType(String name) {
			this.name = name;
		}

		public static ModelType fromString(String v) {
			for (ModelType t : EnumSet.allOf(ModelType.class))
				if (t.name.equalsIgnoreCase(v))
					return t;
			return null;
		}

		public String getName() {
			return name;
		}

		@Override
		public String toString() {
			return getName();
		}
	}

	private static final SingleModel SINGLE_INSTANCE = new SingleModel();
	private static final ArrayModel ARRAY_INSTANCE = new ArrayModel();
	private static final DictionaryModel DICT_INSTANCE = new DictionaryModel();

	public static DataModel getInstance(ModelType mod) {
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
		return getInstance(ModelType.fromString(model));
	}

	public abstract DataModelSpecifier newSpecifier();

	protected abstract LocalKindData newLocalKindData(ResourceID resourceId, DataKind dataKind, BigInteger generationCounter, LocalKinds localKinds);

	public abstract DataModelSpecifier parseSpecifier(UnsignedByteBuffer buf, int length);

	public abstract DataValue parseValue(UnsignedByteBuffer buf, int length);

	public abstract Metadata parseMetadata(UnsignedByteBuffer buf, int length);

	public abstract PreparedValue newPreparedValue(DataKind dataKind);

	public abstract ModelType getModelType();

	@Override
	public String toString() {
		return getModelType().getName();
	}
}
