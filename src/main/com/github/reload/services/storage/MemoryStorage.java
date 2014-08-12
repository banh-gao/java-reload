package com.github.reload.services.storage;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import com.github.reload.components.ComponentsContext;
import com.github.reload.components.ComponentsRepository.Component;
import com.github.reload.net.encoders.header.ResourceID;
import com.github.reload.routing.TopologyPlugin;
import com.google.common.base.Optional;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

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

	private final Multimap<Long, ResourceID> storedKinds = ArrayListMultimap.create();

	private void updateKindToResource(Set<Long> kinds, ResourceID resId) {
		for (Long k : kinds)
			storedKinds.put(k, resId);
	}

	@Override
	public Map<ResourceID, Map<Long, StoreKindData>> getStoredResources() {
		return Collections.unmodifiableMap(storedResources);
	}

	@Override
	public Optional<Map<Long, StoreKindData>> getResource(ResourceID resId) {
		cleanupStore();
		return Optional.fromNullable(storedResources.get(resId));
	}

	@Override
	public void removeResource(ResourceID resourceId) {
		storedResources.remove(resourceId);
	}

	@Override
	public int getSize() {
		return storedResources.size();
	}

	@Override
	public void put(ResourceID resourceId, Map<Long, StoreKindData> values) {
		storedResources.put(resourceId, values);
		updateKindToResource(values.keySet(), resourceId);
		cleanupStore();
	}

	@Override
	public Collection<ResourceID> getResourcesByKind(long kindId) {
		return storedKinds.get(kindId);
	}

	private void cleanupStore() {
		// TODO: perform store cleanup
	}
}