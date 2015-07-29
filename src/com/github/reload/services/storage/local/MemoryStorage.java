package com.github.reload.services.storage.local;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import com.github.reload.net.encoders.header.ResourceID;
import com.google.common.base.Optional;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.SetMultimap;

/**
 * The data stored locally. It stores the data the peer is
 * responsible for.
 * 
 */
public class MemoryStorage implements DataStorage {

	private final Map<ResourceID, Map<Long, StoredKindData>> storedResources = Maps.newConcurrentMap();

	private final SetMultimap<Long, ResourceID> storedKinds = LinkedHashMultimap.create();

	@Override
	public Optional<Map<Long, StoredKindData>> put(ResourceID resourceId, Map<Long, StoredKindData> values) {
		updateKindToResource(values.keySet(), resourceId);
		return Optional.fromNullable(storedResources.put(resourceId, values));
	}

	private void updateKindToResource(Set<Long> kinds, ResourceID resId) {
		for (Long k : kinds) {
			storedKinds.put(k, resId);
		}
	}

	@Override
	public Optional<Map<Long, StoredKindData>> get(ResourceID resId) {
		Optional<Map<Long, StoredKindData>> res = Optional.fromNullable(storedResources.get(resId));

		if (res.isPresent()) {
			deleteExpired(res.get());
		}

		return res;
	}

	private void deleteExpired(Map<Long, StoredKindData> res) {
		for (StoredKindData kd : res.values()) {
			Iterator<StoredData> i = kd.getValues().iterator();
			while (i.hasNext()) {
				StoredData d = i.next();
				if (d.isExpired()) {
					i.remove();
				}
			}
		}
	}

	@Override
	public Optional<Map<Long, StoredKindData>> remove(ResourceID resourceId) {
		return Optional.fromNullable(storedResources.remove(resourceId));
	}

	@Override
	public int size() {
		return storedResources.size();
	}

	@Override
	public Set<ResourceID> keySet() {
		return Collections.unmodifiableSet(storedResources.keySet());
	}

	@Override
	public Set<ResourceID> getResourcesByKind(long kindId) {
		return storedKinds.get(kindId);
	}
}