package com.github.reload.storage;

import java.util.HashMap;
import java.util.Map;

/**
 * Data kind identifier
 * 
 * @author Daniel Zozin <zdenial@gmx.com>
 * 
 */
public class KindId {

	private final long id;

	public final static int MAX_LENGTH = EncUtils.U_INT32;

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

	public static KindId valueOf(UnsignedByteBuffer buf) {
		try {
			KindId kindId = valueOf(buf.getSigned32());
			return kindId;
		} catch (IllegalArgumentException e) {
			throw new DecodingException(e.getMessage());
		}
	}

	public long getId() {
		return id;
	}

	public void writeTo(UnsignedByteBuffer buf) {
		buf.putUnsigned32(id);
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
