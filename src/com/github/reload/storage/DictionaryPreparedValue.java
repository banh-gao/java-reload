package com.github.reload.storage;

import com.github.reload.storage.PreparedData.DataBuildingException;
import com.github.reload.storage.data.DictionaryEntry;
import com.github.reload.storage.data.DictionaryEntry.Key;

/**
 * A dictionary prepared value created by adding a key to a single prepared
 * value
 * 
 * @author Daniel Zozin <zdenial@gmx.com>
 * 
 */
public class DictionaryPreparedValue extends PreparedValue {

	public DictionaryPreparedValue(DataKind dataKind) {
		super(dataKind);
	}

	private Key key;

	public DictionaryPreparedValue setKey(Key key) {
		this.key = key;
		return this;
	}

	public Key getKey() {
		return key;
	}

	@Override
	DictionaryEntry build() throws DataBuildingException {
		if (key == null)
			throw new DataBuildingException("Dictionary key not set");

		return new DictionaryEntry(key.getValue(), getValue(), exists());
	}

	@Override
	public DictionaryPreparedValue setExists(boolean exists) {
		return (DictionaryPreparedValue) super.setExists(exists);
	}

	@Override
	public DictionaryPreparedValue setValue(byte[] value) {
		return (DictionaryPreparedValue) super.setValue(value);
	}
}
