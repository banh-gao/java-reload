package com.github.reload.services.storage.net;

import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import dagger.ObjectGraph;
import com.github.reload.net.codecs.Codec;
import com.github.reload.net.codecs.Codec.ReloadCodec;
import com.github.reload.net.codecs.content.Content;
import com.github.reload.net.codecs.content.ContentType;
import com.github.reload.net.codecs.header.ResourceID;
import com.github.reload.services.storage.local.StoredKindData;
import com.github.reload.services.storage.net.StoreRequest.StoreRequestCodec;

@ReloadCodec(StoreRequestCodec.class)
public class StoreRequest extends Content {

	private final ResourceID resourceId;
	private final short replicaNumber;
	private final Collection<StoredKindData> kindData;

	public StoreRequest(ResourceID id, short replNum, Collection<StoredKindData> data) {
		resourceId = id;
		replicaNumber = replNum;
		kindData = data;
	}

	@Override
	public ContentType getType() {
		return ContentType.STORE_REQ;
	}

	public ResourceID getResourceId() {
		return resourceId;
	}

	public Collection<StoredKindData> getKindData() {
		return kindData;
	}

	public short getReplicaNumber() {
		return replicaNumber;
	}

	static class StoreRequestCodec extends Codec<StoreRequest> {

		private static final int STOREDKINDDATA_LENGTH_FIELD = U_INT32;

		private final Codec<ResourceID> resIdCodec;
		private final Codec<StoredKindData> storeKindDataCodec;

		public StoreRequestCodec(ObjectGraph ctx) {
			super(ctx);
			resIdCodec = getCodec(ResourceID.class);
			storeKindDataCodec = getCodec(StoredKindData.class);
		}

		@Override
		public void encode(StoreRequest obj, ByteBuf buf, Object... params) throws com.github.reload.net.codecs.Codec.CodecException {
			resIdCodec.encode(obj.resourceId, buf);

			buf.writeByte(obj.replicaNumber);

			Field lenFld = allocateField(buf, STOREDKINDDATA_LENGTH_FIELD);

			for (StoredKindData d : obj.kindData) {
				storeKindDataCodec.encode(d, buf);
			}

			lenFld.updateDataLength();
		}

		@Override
		public StoreRequest decode(ByteBuf buf, Object... params) throws com.github.reload.net.codecs.Codec.CodecException {
			ResourceID resId = resIdCodec.decode(buf);
			short replicaNumber = buf.readUnsignedByte();
			List<StoredKindData> kindData = decodeKindDataList(buf);
			return new StoreRequest(resId, replicaNumber, kindData);
		}

		private List<StoredKindData> decodeKindDataList(ByteBuf buf) throws com.github.reload.net.codecs.Codec.CodecException {
			List<StoredKindData> out = new ArrayList<StoredKindData>();

			ByteBuf kindData = readField(buf, STOREDKINDDATA_LENGTH_FIELD);

			while (kindData.readableBytes() > 0) {
				out.add(storeKindDataCodec.decode(kindData));
			}

			kindData.release();
			return out;
		}
	}
}
