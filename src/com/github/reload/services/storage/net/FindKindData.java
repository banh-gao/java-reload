package com.github.reload.services.storage.net;

import io.netty.buffer.ByteBuf;
import java.util.Objects;
import dagger.ObjectGraph;
import com.github.reload.net.codecs.Codec;
import com.github.reload.net.codecs.Codec.ReloadCodec;
import com.github.reload.net.codecs.header.ResourceID;
import com.github.reload.services.storage.DataKind;
import com.github.reload.services.storage.net.FindKindData.FindKindDataCodec;

/**
 * Find data contained in a find answer for a specific kind-id
 * 
 */
@ReloadCodec(FindKindDataCodec.class)
public class FindKindData {

	private final DataKind kind;
	private final ResourceID resourceId;

	public FindKindData(DataKind kind, ResourceID resourceId) {
		this.kind = kind;
		this.resourceId = resourceId;
	}

	public DataKind getKind() {
		return kind;
	}

	public ResourceID getResourceId() {
		return resourceId;
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), kind, resourceId);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		FindKindData other = (FindKindData) obj;
		if (kind == null) {
			if (other.kind != null)
				return false;
		} else if (!kind.equals(other.kind))
			return false;
		if (resourceId == null) {
			if (other.resourceId != null)
				return false;
		} else if (!resourceId.equals(other.resourceId))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "FindKindData [kind=" + kind + ", resourceId=" + resourceId + "]";
	}

	static class FindKindDataCodec extends Codec<FindKindData> {

		private final Codec<DataKind> kindCodec;
		private final Codec<ResourceID> resIdCodec;

		public FindKindDataCodec(ObjectGraph ctx) {
			super(ctx);
			kindCodec = getCodec(DataKind.class);
			resIdCodec = getCodec(ResourceID.class);
		}

		@Override
		public void encode(FindKindData obj, ByteBuf buf, Object... params) throws CodecException {
			kindCodec.encode(obj.kind, buf);
			resIdCodec.encode(obj.resourceId, buf);
		}

		@Override
		public FindKindData decode(ByteBuf buf, Object... params) throws CodecException {
			DataKind kind = kindCodec.decode(buf);
			ResourceID resId = resIdCodec.decode(buf);
			return new FindKindData(kind, resId);
		}

	}

}
