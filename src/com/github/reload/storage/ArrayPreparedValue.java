package com.github.reload.storage;

import com.github.reload.storage.PreparedData.DataBuildingException;
import com.github.reload.storage.data.ArrayEntry;

/**
 * An array prepared value created by adding an index to a single prepared value
 * 
 * @author Daniel Zozin <zdenial@gmx.com>
 * 
 */
public class ArrayPreparedValue extends PreparedValue {

	public ArrayPreparedValue(DataKind dataKind) {
		super(dataKind);
	}

	public ArrayPreparedValue(DataKind dataKind, ArrayPreparedValue v) {
		super(dataKind);
		index = v.index;
		setExists(v.exists());
		setValue(v.getValue());
	}

	private long index = -1;

	public ArrayPreparedValue setIndex(long index) {
		this.index = index;
		return this;
	}

	public long getIndex() {
		return index;
	}

	@Override
	ArrayEntry build() {
		if (index == -1)
			throw new DataBuildingException("Array index not set");
		return new ArrayEntry(index, getValue(), exists());
	}

	@Override
	public ArrayPreparedValue setValue(byte[] value) {
		return (ArrayPreparedValue) super.setValue(value);
	}

	@Override
	public ArrayPreparedValue setExists(boolean exists) {
		return (ArrayPreparedValue) super.setExists(exists);
	}
}