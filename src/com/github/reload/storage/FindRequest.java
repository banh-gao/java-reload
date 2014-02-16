package com.github.reload.storage;

import io.netty.buffer.ByteBuf;
import java.util.LinkedHashSet;
import java.util.Set;
import com.github.reload.Context;
import com.github.reload.message.Content;
import com.github.reload.message.ContentType;
import com.github.reload.message.ResourceID;
import com.github.reload.message.errors.InvalidMessageException;
import com.github.reload.net.data.Codec;
import com.github.reload.net.data.ReloadCodec;
import com.github.reload.storage.FindRequest.FindRequestCodec;

@ReloadCodec(FindRequestCodec.class)
public class FindRequest extends Content {

	private final ResourceID resourceId;
	private final Set<KindId> kinds;

	public FindRequest(ResourceID resourceId, Set<KindId> kinds) {
		this.resourceId = resourceId;
		this.kinds = new LinkedHashSet<KindId>();
		this.kinds.addAll(kinds);
	}

	@Override
	public ContentType getType() {
		return ContentType.FIND_REQ;
	}

	public ResourceID getResourceId() {
		return resourceId;
	}

	public Set<KindId> getKinds() {
		return kinds;
	}

	public static class FindRequestCodec extends Codec<FindRequest> {

		private static final int KINDS_LENGTH_FIELD = U_INT8;

		private final Codec<ResourceID> resIdCodec;
		private final Codec<KindId> kindIdCodec;

		public FindRequestCodec(Context context) {
			super(context);
			resIdCodec = getCodec(ResourceID.class);
			kindIdCodec = getCodec(KindId.class);
		}

		@Override
		public void encode(FindRequest obj, ByteBuf buf) throws CodecException {
			resIdCodec.encode(obj.resourceId, buf);
			Field lenFld = allocateField(buf, KINDS_LENGTH_FIELD);

			for (KindId k : obj.kinds) {
				kindIdCodec.encode(k, buf);
			}

			lenFld.updateDataLength();
		}

		@Override
		public FindRequest decode(ByteBuf buf) throws CodecException {
			ResourceID resourceId = resIdCodec.decode(buf);

			Set<KindId> kinds = new LinkedHashSet<KindId>();

			ByteBuf kindIdData = readField(buf, KINDS_LENGTH_FIELD);

			while (kindIdData.readableBytes() > 0) {
				KindId kind = kindIdCodec.decode(kindIdData);
				if (!kinds.add(kind))
					throw new InvalidMessageException("Duplicate kind-id " + kind.getId());
			}

			return new FindRequest(resourceId, kinds);
		}

	}
}
