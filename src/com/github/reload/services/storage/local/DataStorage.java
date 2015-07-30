package com.github.reload.services.storage.local;

import java.util.Map;
import java.util.Set;
import com.github.reload.net.codecs.header.ResourceID;
import com.google.common.base.Optional;

public interface DataStorage {

	public Optional<Map<Long, StoredKindData>> put(ResourceID resourceId, Map<Long, StoredKindData> value);

	public Optional<Map<Long, StoredKindData>> get(ResourceID resId);

	public Optional<Map<Long, StoredKindData>> remove(ResourceID resourceId);

	public int size();

	public Set<ResourceID> keySet();

	public Set<ResourceID> getResourcesByKind(long kindId);

}