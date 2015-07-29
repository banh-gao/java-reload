package com.github.reload.services.storage.net;

import io.netty.buffer.ByteBuf;
import java.util.LinkedHashSet;
import java.util.Set;
import com.github.reload.components.ComponentsContext;
import com.github.reload.net.encoders.Codec;
import com.github.reload.net.encoders.Codec.ReloadCodec;
import com.github.reload.net.encoders.content.Content;
import com.github.reload.net.encoders.content.ContentType;
import com.github.reload.net.encoders.header.ResourceID;
import com.github.reload.services.storage.DataKind;
import com.github.reload.services.storage.net.FindRequest.FindRequestCodec;

@ReloadCodec(FindRequestCodec.class)
public class FindRequest extends Content {

	private final ResourceID resourceId;
	private final Set<DataKind> kinds;

	public FindRequest(ResourceID resourceId, Set<DataKind> kinds) {
		this.resourceId = resourceId;
		this.kinds = new LinkedHashSet<DataKind>();
		this.kinds.addAll(kinds);
	}

	@Override
	public ContentType getType() {
		return ContentType.FIND_REQ;
	}

	public ResourceID getResourceId() {
		return resourceId;
	}

	public Set<DataKind> getKinds() {
		return kinds;
	}

	static class FindRequestCodec extends Codec<FindRequest> {

		private static final int KINDS_LENGTH_FIELD = U_INT8;

		private final Codec<ResourceID> resIdCodec;
		private final Codec<DataKind> kindCodec;

		public FindRequestCodec(ComponentsContext ctx) {
			super(ctx);
			resIdCodec = getCodec(ResourceID.class);
			kindCodec = getCodec(DataKind.class);
		}

		@Override
		public void encode(FindRequest obj, ByteBuf buf, Object... params) throws CodecException {
			resIdCodec.encode(obj.resourceId, buf);
			Field lenFld = allocateField(buf, KINDS_LENGTH_FIELD);

			for (DataKind k : obj.kinds) {
				kindCodec.encode(k, buf);
			}

			lenFld.updateDataLength();
		}

		@Override
		public FindRequest decode(ByteBuf buf, Object... params) throws CodecException {
			ResourceID resourceId = resIdCodec.decode(buf);

			Set<DataKind> kinds = new LinkedHashSet<DataKind>();

			ByteBuf kindIdData = readField(buf, KINDS_LENGTH_FIELD);

			while (kindIdData.readableBytes() > 0) {
				DataKind kind = kindCodec.decode(kindIdData);
				if (!kinds.add(kind))
					throw new CodecException("Duplicate kind-id " + kind.getKindId());
			}

			return new FindRequest(resourceId, kinds);
		}

	}
}
