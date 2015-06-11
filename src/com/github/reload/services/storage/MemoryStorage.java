package com.github.reload.services.storage;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import com.github.reload.components.ComponentsContext;
import com.github.reload.components.ComponentsRepository.Component;
import com.github.reload.net.encoders.header.ResourceID;
import com.github.reload.routing.TopologyPlugin;
import com.github.reload.services.storage.encoders.StoredData;
import com.google.common.base.Optional;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.SetMultimap;

/**
 * The data stored locally. It stores the data the peer is
 * responsible for.
 * 
 */
@Component(value = DataStorage.class, priority = 1)
public class MemoryStorage implements DataStorage {

	@Component
	private ComponentsContext ctx;

	@Component
	private TopologyPlugin plugin;

	private final Map<ResourceID, Map<Long, StoreKindData>> storedResources = Maps.newConcurrentMap();

	private final SetMultimap<Long, ResourceID> storedKinds = LinkedHashMultimap.create();

	@Override
	public Optional<Map<Long, StoreKindData>> put(ResourceID resourceId, Map<Long, StoreKindData> values) {
		updateKindToResource(values.keySet(), resourceId);
		return Optional.fromNullable(storedResources.put(resourceId, values));
	}

	private void updateKindToResource(Set<Long> kinds, ResourceID resId) {
		for (Long k : kinds)
			storedKinds.put(k, resId);
	}

	@Override
	public Optional<Map<Long, StoreKindData>> get(ResourceID resId) {
		Optional<Map<Long, StoreKindData>> res = Optional.fromNullable(storedResources.get(resId));

		if (res.isPresent())
			deleteExpired(res.get());

		return res;
	}

	private void deleteExpired(Map<Long, StoreKindData> res) {
		for (StoreKindData kd : res.values()) {
			Iterator<StoredData> i = kd.getValues().iterator();
			while (i.hasNext()) {
				StoredData d = i.next();
				if (d.isExpired())
					i.remove();
			}
		}
	}

	@Override
	public Optional<Map<Long, StoreKindData>> remove(ResourceID resourceId) {
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