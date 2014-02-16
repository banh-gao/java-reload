package com.github.reload.storage;

import com.github.reload.storage.DataModel.ModelType;

/**
 * A model specifier used to fetch data of a particular data model
 * 
 * @author Daniel Zozin <zdenial@gmx.com>
 * 
 */
public class DataModelSpecifier {

	private final ModelType modelType;

	DataModelSpecifier(ModelType modelType) {
		this.modelType = modelType;
	}

	/**
	 * @return The data model related with this specifier
	 */
	ModelType getModelType() {
		return modelType;
	}

	/**
	 * Write the specifier to the passed buffer
	 * 
	 * @param buf
	 *            the buffer where to write to
	 */
	void writeTo(UnsignedByteBuffer buf) {
		// No content
	}
}