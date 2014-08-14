package com.github.reload.services.storage;

import java.util.Map;
import java.util.Set;
import com.github.reload.net.encoders.header.ResourceID;
import com.google.common.base.Optional;

public interface DataStorage {

	public Optional<Map<Long, StoreKindData>> put(ResourceID resourceId, Map<Long, StoreKindData> value);

	public Optional<Map<Long, StoreKindData>> get(ResourceID resId);

	public Optional<Map<Long, StoreKindData>> remove(ResourceID resourceId);

	public int size();

	public Set<ResourceID> keySet();

	public Set<ResourceID> getResourcesByKind(long kindId);

}