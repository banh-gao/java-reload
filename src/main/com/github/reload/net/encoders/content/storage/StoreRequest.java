package com.github.reload.net.encoders.content.storage;

import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import com.github.reload.Configuration;
import com.github.reload.net.encoders.Codec;
import com.github.reload.net.encoders.Codec.ReloadCodec;
import com.github.reload.net.encoders.content.Content;
import com.github.reload.net.encoders.content.ContentType;
import com.github.reload.net.encoders.content.storage.StoreRequest.StoreRequestCodec;
import com.github.reload.net.encoders.header.ResourceID;

@ReloadCodec(StoreRequestCodec.class)
public class StoreRequest extends Content {

	private final ResourceID resourceId;
	private final short replicaNumber;
	private final List<StoreKindData> kindData;

	public StoreRequest(ResourceID id, short replicaNumber, List<StoreKindData> data) {
		resourceId = id;
		this.replicaNumber = replicaNumber;
		kindData = data;
	}

	@Override
	public ContentType getType() {
		return ContentType.STORE_REQ;
	}

	public ResourceID getResourceId() {
		return resourceId;
	}

	public List<StoreKindData> getKindData() {
		return kindData;
	}

	public short getReplicaNumber() {
		return replicaNumber;
	}

	static class StoreRequestCodec extends Codec<StoreRequest> {

		private static final int STOREDKINDDATA_LENGTH_FIELD = U_INT32;

		private final Codec<ResourceID> resIdCodec;
		private final Codec<StoreKindData> storeKindDataCodec;

		public StoreRequestCodec(Configuration conf) {
			super(conf);
			resIdCodec = getCodec(ResourceID.class);
			storeKindDataCodec = getCodec(StoreKindData.class);
		}

		@Override
		public void encode(StoreRequest obj, ByteBuf buf, Object... params) throws com.github.reload.net.encoders.Codec.CodecException {
			resIdCodec.encode(obj.resourceId, buf);

			buf.writeByte(obj.replicaNumber);

			Field lenFld = allocateField(buf, STOREDKINDDATA_LENGTH_FIELD);

			for (StoreKindData d : obj.kindData) {
				storeKindDataCodec.encode(d, buf);
			}

			lenFld.updateDataLength();
		}

		@Override
		public StoreRequest decode(ByteBuf buf, Object... params) throws com.github.reload.net.encoders.Codec.CodecException {
			ResourceID resId = resIdCodec.decode(buf);
			short replicaNumber = buf.readUnsignedByte();
			List<StoreKindData> kindData = decodeKindDataList(buf);
			return new StoreRequest(resId, replicaNumber, kindData);
		}

		private List<StoreKindData> decodeKindDataList(ByteBuf buf) throws com.github.reload.net.encoders.Codec.CodecException {
			List<StoreKindData> out = new ArrayList<StoreKindData>();

			ByteBuf kindData = readField(buf, STOREDKINDDATA_LENGTH_FIELD);

			while (kindData.readableBytes() > 0) {
				out.add(storeKindDataCodec.decode(kindData));
			}

			kindData.release();
			return out;
		}
	}
}
