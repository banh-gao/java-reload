package com.github.reload.storage;

import com.github.reload.storage.PreparedData.DataBuildingException;
import com.github.reload.storage.data.DataValue;

/**
 * A prepared value used to store values of a generic data model
 * 
 * @author Daniel Zozin <zdenial@gmx.com>
 * 
 */
public class PreparedValue {

	protected final DataKind dataKind;

	private byte[] value = new byte[0];
	private boolean exists = true;

	PreparedValue(DataKind dataKind) {
		this.dataKind = dataKind;
	}

	public DataKind getDataKind() {
		return dataKind;
	}

	/**
	 * Set the raw data to be stored
	 */
	public PreparedValue setValue(byte[] value) {
		this.value = value;
		return this;
	}

	/**
	 * Set the existence status
	 */
	public PreparedValue setExists(boolean exists) {
		this.exists = exists;
		return this;
	}

	/**
	 * @return The stored value
	 */
	public byte[] getValue() {
		return value;
	}

	/**
	 * @return The existence status of this value
	 */
	public boolean exists() {
		return exists;
	}

	/**
	 * @return the generated value
	 * @throws DataBuildingException
	 */
	DataValue build() throws DataBuildingException {
		return new DataValue(value, exists);
	}
}
