package com.github.reload.storage;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import com.github.reload.message.ResourceID;
import com.github.reload.storage.DictionaryValue.Key;

/**
 * Class used into the local storage to store array values
 * 
 * @author Daniel Zozin <zdenial@gmx.com>
 * 
 */
public class DictionaryLocalKindData extends LocalKindData {

	Map<Key, StoredData> values = new HashMap<DictionaryValue.Key, StoredData>();

	protected DictionaryLocalKindData(ResourceID resourceId, DataKind kind, BigInteger generationCounter, LocalKinds localKinds) {
		super(resourceId, kind, generationCounter, localKinds);
	}

	@Override
	protected void implAdd(StoredData data) {
		values.put(((DictionaryValue) data.getValue()).getKey(), data);
	}

	@Override
	protected StoredData getStoredObjectFor(StoredData data) {
		return values.get(((DictionaryValue) data.getValue()).getKey());
	}

	@Override
	protected boolean contains(StoredData data) {
		return values.containsKey(((DictionaryValue) data.getValue()).getKey());
	}

	@Override
	public int size() {
		return values.size();
	}

	@Override
	protected void implRemove(StoredData data) {
		values.remove(((DictionaryValue) data.getValue()).getKey());
	}

	@Override
	public List<StoredData> getMatchingDataValues(DataModelSpecifier spec) {
		if (!(spec instanceof DictionaryModelSpecifier))
			throw new IllegalArgumentException("Wrong model specifier");

		DictionaryModelSpecifier dictSpec = (DictionaryModelSpecifier) spec;

		// Return the entire dictionary
		if (dictSpec.getKeys().size() == 0)
			return new ArrayList<StoredData>(values.values());

		List<StoredData> out = new ArrayList<StoredData>();

		for (Key k : dictSpec.getKeys()) {
			StoredData v = values.get(k);
			if (v != null) {
				out.add(v);
			}
		}

		return out;
	}

	@Override
	public List<StoredMetadata> getMatchingMetaDataValues(DataModelSpecifier spec) {
		if (!(spec instanceof DictionaryModelSpecifier))
			throw new IllegalArgumentException("Wrong model specifier");

		DictionaryModelSpecifier dictSpec = (DictionaryModelSpecifier) spec;

		List<StoredMetadata> out = new ArrayList<StoredMetadata>();

		for (Key k : dictSpec.getKeys()) {
			StoredData v = values.get(k);
			if (v != null) {
				out.add(v.getMetadata());
			}
		}

		return out;
	}

	@Override
	protected void changeDataKind(DataKind updatedKind, Context context) {
		Iterator<StoredData> i = values.values().iterator();
		while (i.hasNext()) {
			StoredData currentData = i.next();
			if (currentData.getKind().equals(updatedKind)) {
				continue;
			}
			try {
				currentData.updateDataKind(resourceId, updatedKind, context);
			} catch (StorageException e) {
				i.remove();
			}
		}
	}

	@Override
	public List<StoredData> getDataValues() {
		return new ArrayList<StoredData>(values.values());
	}

}
