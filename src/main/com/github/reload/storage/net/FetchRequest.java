package com.github.reload.storage.net;

import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import com.github.reload.Configuration;
import com.github.reload.message.Codec;
import com.github.reload.message.Content;
import com.github.reload.message.ContentType;
import com.github.reload.message.Codec.ReloadCodec;
import com.github.reload.message.ResourceID;
import com.github.reload.storage.data.StoredDataSpecifier;
import com.github.reload.storage.net.FetchRequest.FetchRequestCodec;

@ReloadCodec(FetchRequestCodec.class)
public class FetchRequest extends Content {

	private final ResourceID resourceId;
	private final List<StoredDataSpecifier> specifiers;

	public FetchRequest(ResourceID resId, List<StoredDataSpecifier> specifiers) {
		resourceId = resId;
		this.specifiers = specifiers;
	}

	public ResourceID getResourceId() {
		return resourceId;
	}

	public List<StoredDataSpecifier> getSpecifiers() {
		return specifiers;
	}

	@Override
	public ContentType getType() {
		return ContentType.FETCH_REQ;
	}

	public static class FetchRequestCodec extends Codec<FetchRequest> {

		private static final int SPECIFIERS_LENGTH_FIELD = U_INT16;

		private final Codec<ResourceID> resIdCodec;
		private final Codec<StoredDataSpecifier> dataSpecifierCodec;

		public FetchRequestCodec(Configuration conf) {
			super(conf);
			resIdCodec = getCodec(ResourceID.class);
			dataSpecifierCodec = getCodec(StoredDataSpecifier.class);
		}

		@Override
		public void encode(FetchRequest obj, ByteBuf buf, Object... params) throws CodecException {
			resIdCodec.encode(obj.resourceId, buf);
			encodeSpecifiers(obj, buf);
		}

		private void encodeSpecifiers(FetchRequest obj, ByteBuf buf) throws com.github.reload.message.Codec.CodecException {
			Field lenFld = allocateField(buf, SPECIFIERS_LENGTH_FIELD);

			for (StoredDataSpecifier s : obj.specifiers) {
				dataSpecifierCodec.encode(s, buf);
			}

			lenFld.updateDataLength();
		}

		@Override
		public FetchRequest decode(ByteBuf buf, Object... params) throws CodecException {
			ResourceID resourceId = resIdCodec.decode(buf);
			List<StoredDataSpecifier> specifiers = decodeSpecifiers(buf);
			return new FetchRequest(resourceId, specifiers);
		}

		private List<StoredDataSpecifier> decodeSpecifiers(ByteBuf buf) throws com.github.reload.message.Codec.CodecException {
			List<StoredDataSpecifier> out = new ArrayList<StoredDataSpecifier>();

			ByteBuf specifiersData = readField(buf, SPECIFIERS_LENGTH_FIELD);
			while (specifiersData.readableBytes() > 0) {
				out.add(dataSpecifierCodec.decode(specifiersData));
			}
			specifiersData.release();

			return out;
		}

	}
}
