package com.github.reload.storage;

import java.util.HashMap;
import java.util.Map;

/**
 * Data kind identifier
 * 
 */
public class KindId {

	private final long id;

	private final static Map<Long, KindId> INSTANCES = new HashMap<Long, KindId>();

	private KindId(long id) {
		this.id = id;
	}

	public static KindId valueOf(long id) {
		if (id <= 0)
			throw new IllegalArgumentException("Invalid kind id");
		KindId kindId = INSTANCES.get(id);
		if (kindId == null) {
			kindId = new KindId(id);
			INSTANCES.put(id, kindId);
		}
		return kindId;
	}

	public long getId() {
		return id;
	}

	@Override
	public int hashCode() {
		return Long.valueOf(id).hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		KindId other = (KindId) obj;
		return Long.valueOf(id).equals(other.id);
	}

	@Override
	public String toString() {
		return "KindId[" + id + "]";
	}

}
