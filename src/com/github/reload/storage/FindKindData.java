package com.github.reload.storage;

import com.github.reload.message.ResourceID;

/**
 * Find data contained in a find answer for a specific kind-id
 * 
 * @author Daniel Zozin <zdenial@gmx.com>
 * 
 */
public class FindKindData {

	private final KindId kind;
	private final ResourceID resourceId;

	public FindKindData(KindId kind, ResourceID resourceId) {
		this.kind = kind;
		this.resourceId = resourceId;
	}

	public FindKindData(UnsignedByteBuffer buf) {
		kind = KindId.valueOf(buf);
		resourceId = ResourceID.valueOf(buf);
	}

	public KindId getKindId() {
		return kind;
	}

	public ResourceID getResourceId() {
		return resourceId;
	}

	public void writeTo(UnsignedByteBuffer buf) {
		kind.writeTo(buf);
		resourceId.writeTo(buf);
	}

	@Override
	public String toString() {
		return "FindKindData [kind=" + kind + ", resourceId=" + resourceId + "]";
	}
}
