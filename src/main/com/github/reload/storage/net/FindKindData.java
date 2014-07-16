package com.github.reload.storage.net;

import io.netty.buffer.ByteBuf;
import com.github.reload.Configuration;
import com.github.reload.DataKind;
import com.github.reload.net.encoders.Codec;
import com.github.reload.net.encoders.Codec.ReloadCodec;
import com.github.reload.net.encoders.header.ResourceID;
import com.github.reload.storage.net.FindKindData.FindKindDataCodec;

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
	public String toString() {
		return "FindKindData [kind=" + kind + ", resourceId=" + resourceId + "]";
	}

	public static class FindKindDataCodec extends Codec<FindKindData> {

		private final Codec<DataKind> kindCodec;
		private final Codec<ResourceID> resIdCodec;

		public FindKindDataCodec(Configuration conf) {
			super(conf);
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
