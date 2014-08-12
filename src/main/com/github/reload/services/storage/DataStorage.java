package com.github.reload.services.storage;

import java.util.Collection;
import java.util.Map;
import com.github.reload.net.encoders.header.ResourceID;
import com.google.common.base.Optional;

public interface DataStorage {

	public abstract Map<ResourceID, Map<Long, StoreKindData>> getStoredResources();

	public abstract Optional<Map<Long, StoreKindData>> getResource(ResourceID resId);

	public abstract void removeResource(ResourceID resourceId);

	public abstract int getSize();

	public abstract void put(ResourceID resourceId, Map<Long, StoreKindData> tempStore);

	public abstract Collection<ResourceID> getResourcesByKind(long kindId);

}