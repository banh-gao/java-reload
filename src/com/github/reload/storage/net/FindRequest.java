package com.github.reload.storage.net;

import io.netty.buffer.ByteBuf;
import java.util.LinkedHashSet;
import java.util.Set;
import com.github.reload.Configuration;
import com.github.reload.DataKind;
import com.github.reload.message.Content;
import com.github.reload.message.ContentType;
import com.github.reload.message.ResourceID;
import com.github.reload.message.errors.InvalidMessageException;
import com.github.reload.net.data.Codec;
import com.github.reload.net.data.ReloadCodec;
import com.github.reload.storage.net.FindRequest.FindRequestCodec;

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

	public static class FindRequestCodec extends Codec<FindRequest> {

		private static final int KINDS_LENGTH_FIELD = U_INT8;

		private final Codec<ResourceID> resIdCodec;
		private final Codec<DataKind> kindCodec;

		public FindRequestCodec(Configuration conf) {
			super(conf);
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
					throw new InvalidMessageException("Duplicate kind-id " + kind.getKindId());
			}

			return new FindRequest(resourceId, kinds);
		}

	}
}
