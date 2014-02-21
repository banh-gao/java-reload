package com.github.reload.storage;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import com.github.reload.message.GenericSignature;
import com.github.reload.message.ResourceID;
import com.github.reload.message.SignerIdentity;
import com.github.reload.storage.ArrayModelSpecifier.ArrayRange;
import com.github.reload.storage.data.ArrayMetadata;
import com.github.reload.storage.data.ArrayValue;
import com.github.reload.storage.data.StoredData;
import com.github.reload.storage.data.StoredMetadata;
import com.github.reload.storage.errors.DataTooLargeException;

/**
 * Class used into the local storage to store array values
 * 
 * @author Daniel Zozin <zdenial@gmx.com>
 * 
 */
public class ArrayLocalKindData extends LocalKindData {

	private final TreeMap<Long, StoredData> values = new TreeMap<Long, StoredData>();

	protected ArrayLocalKindData(ResourceID resourceId, DataKind kind, BigInteger generationCounter, LocalKinds localKinds) {
		super(resourceId, kind, generationCounter, localKinds);
	}

	@Override
	protected void implAdd(StoredData data) throws DataTooLargeException {
		long newIndex = ((ArrayValue) data.getValue()).getIndex();

		if (newIndex == ArrayModel.LAST_INDEX) {
			long index = (values.isEmpty()) ? 0 : values.lastKey() + 1;
			((ArrayValue) data.getValue()).index = index;
			values.put(index, data);
		} else if (newIndex <= kind.getLongAttribute(DataKind.ATTR_MAX_COUNT)) {
			values.put(((ArrayValue) data.getValue()).getIndex(), data);
		} else
			throw new DataTooLargeException("The array index will exceed the kind maximum allowed array size");
	}

	@Override
	protected StoredData getStoredObjectFor(StoredData data) {
		return values.get(((ArrayValue) data.getValue()).getIndex());
	}

	@Override
	protected boolean contains(StoredData data) {
		return values.containsKey(((ArrayValue) data.getValue()).getIndex());
	}

	@Override
	public int size() {
		return values.size();
	}

	@Override
	protected void implRemove(StoredData data) {
		values.remove(((ArrayValue) data.getValue()).getIndex());
	}

	@Override
	public List<StoredData> getMatchingDataValues(DataModelSpecifier spec) {
		if (!(spec instanceof ArrayModelSpecifier))
			throw new IllegalArgumentException("Wrong model specifier");

		ArrayModelSpecifier arraySpec = (ArrayModelSpecifier) spec;

		TreeMap<Long, StoredData> out = new TreeMap<Long, StoredData>();

		for (ArrayRange r : arraySpec.getRanges()) {
			for (long i = r.getStartIndex(); i <= r.getEndIndex(); i++) {

				StoredData v;

				if (i == 0) {
					v = values.get(values.firstKey());
				} else if (i == ArrayModel.LAST_INDEX) {
					v = values.get(values.lastKey());
				} else {
					v = values.get(i);
				}

				if (v != null) {
					out.put(i, v);
				} else if (values.lastKey() > i) {
					out.put(i, buildEmptyDataValue(i));
				}
			}
		}

		return new ArrayList<StoredData>(out.values());
	}

	private StoredData buildEmptyDataValue(long index) {
		return new StoredData(kind, BigInteger.ZERO, 0, new ArrayValue(index, new byte[0], false), GenericSignature.EMPTY_SIGNATURE);
	}

	@Override
	public List<StoredMetadata> getMatchingMetaDataValues(DataModelSpecifier spec) {
		if (!(spec instanceof ArrayModelSpecifier))
			throw new IllegalArgumentException("Wrong model specifier");

		ArrayModelSpecifier arraySpec = (ArrayModelSpecifier) spec;

		Set<StoredMetadata> out = new HashSet<StoredMetadata>();

		for (ArrayRange r : arraySpec.getRanges()) {
			for (long i = r.getStartIndex(); i <= r.getEndIndex(); i++) {
				StoredData v = values.get(i);
				if (v != null) {
					out.add(v.getMetadata());
				} else {
					out.add(buildPaddingMetadataValue(i));
				}
			}
		}

		return new ArrayList<StoredMetadata>(out);
	}

	private StoredMetadata buildPaddingMetadataValue(long index) {
		return new StoredMetadata(kind, BigInteger.ZERO, 0, new ArrayMetadata(new ArrayValue(index, new byte[0], false)), SignerIdentity.EMPTY_IDENTITY);
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
