package com.github.reload.storage;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import com.github.reload.message.ResourceID;

/**
 * Class used into the local storage to store a single value
 * 
 * @author Daniel Zozin <zdenial@gmx.com>
 * 
 */
public class SingleLocalKindData extends LocalKindData {

	private StoredData value;

	SingleLocalKindData(ResourceID resourceId, DataKind kind, BigInteger generationCounter, LocalKinds localKinds) {
		super(resourceId, kind, generationCounter, localKinds);
	}

	@Override
	protected void implAdd(StoredData data) {
		value = data;
	}

	@Override
	protected StoredData getStoredObjectFor(StoredData data) {
		return value;
	}

	@Override
	protected boolean contains(StoredData data) {
		return value != null;
	}

	@Override
	public int size() {
		return value == null ? 0 : 1;
	}

	@Override
	protected void implRemove(StoredData data) {
		if (data != null) {
			value = null;
		}
	}

	@Override
	public List<StoredData> getMatchingDataValues(DataModelSpecifier spec) {
		List<StoredData> v = new ArrayList<StoredData>();
		if (value != null) {
			v.add(value);
		}
		return v;
	}

	@Override
	public List<StoredMetadata> getMatchingMetaDataValues(DataModelSpecifier spec) {
		List<StoredMetadata> v = new ArrayList<StoredMetadata>();
		if (value != null) {
			v.add(value.getMetadata());
		}
		return v;
	}

	@Override
	protected void changeDataKind(DataKind updatedKind, Context context) {
		if (value == null)
			return;

		try {
			value.updateDataKind(resourceId, updatedKind, context);
		} catch (StorageException e) {
			value = null;
		}
	}

	@Override
	public List<StoredData> getDataValues() {
		List<StoredData> out = new ArrayList<StoredData>();
		if (value != null) {
			out.add(value);
		}
		return out;
	}
}
