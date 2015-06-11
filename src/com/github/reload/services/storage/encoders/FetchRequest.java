package com.github.reload.services.storage.encoders;

import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import com.github.reload.components.ComponentsContext;
import com.github.reload.net.encoders.Codec;
import com.github.reload.net.encoders.Codec.ReloadCodec;
import com.github.reload.net.encoders.content.Content;
import com.github.reload.net.encoders.content.ContentType;
import com.github.reload.net.encoders.header.ResourceID;
import com.github.reload.services.storage.encoders.FetchRequest.FetchRequestCodec;

@ReloadCodec(FetchRequestCodec.class)
public class FetchRequest extends Content {

	private final ResourceID resourceId;
	private final List<StoreKindDataSpecifier> specifiers;

	public FetchRequest(ResourceID resId, List<StoreKindDataSpecifier> specifiers) {
		resourceId = resId;
		this.specifiers = specifiers;
	}

	public ResourceID getResourceId() {
		return resourceId;
	}

	public List<StoreKindDataSpecifier> getSpecifiers() {
		return specifiers;
	}

	@Override
	public ContentType getType() {
		return ContentType.FETCH_REQ;
	}

	static class FetchRequestCodec extends Codec<FetchRequest> {

		private static final int SPECIFIERS_LENGTH_FIELD = U_INT16;

		private final Codec<ResourceID> resIdCodec;
		private final Codec<StoreKindDataSpecifier> dataSpecifierCodec;

		public FetchRequestCodec(ComponentsContext ctx) {
			super(ctx);
			resIdCodec = getCodec(ResourceID.class);
			dataSpecifierCodec = getCodec(StoreKindDataSpecifier.class);
		}

		@Override
		public void encode(FetchRequest obj, ByteBuf buf, Object... params) throws CodecException {
			resIdCodec.encode(obj.resourceId, buf);
			encodeSpecifiers(obj, buf);
		}

		private void encodeSpecifiers(FetchRequest obj, ByteBuf buf) throws com.github.reload.net.encoders.Codec.CodecException {
			Field lenFld = allocateField(buf, SPECIFIERS_LENGTH_FIELD);

			for (StoreKindDataSpecifier s : obj.specifiers) {
				dataSpecifierCodec.encode(s, buf);
			}

			lenFld.updateDataLength();
		}

		@Override
		public FetchRequest decode(ByteBuf buf, Object... params) throws CodecException {
			ResourceID resourceId = resIdCodec.decode(buf);
			List<StoreKindDataSpecifier> specifiers = decodeSpecifiers(buf);
			return new FetchRequest(resourceId, specifiers);
		}

		private List<StoreKindDataSpecifier> decodeSpecifiers(ByteBuf buf) throws com.github.reload.net.encoders.Codec.CodecException {
			List<StoreKindDataSpecifier> out = new ArrayList<StoreKindDataSpecifier>();

			ByteBuf specifiersData = readField(buf, SPECIFIERS_LENGTH_FIELD);
			while (specifiersData.readableBytes() > 0) {
				out.add(dataSpecifierCodec.decode(specifiersData));
			}
			specifiersData.release();

			return out;
		}

	}
}
