package com.github.reload.services.storage.net;

import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import dagger.ObjectGraph;
import com.github.reload.net.codecs.Codec;
import com.github.reload.net.codecs.Codec.ReloadCodec;
import com.github.reload.net.codecs.content.Content;
import com.github.reload.net.codecs.content.ContentType;
import com.github.reload.net.codecs.header.ResourceID;
import com.github.reload.services.storage.net.FetchRequest.FetchRequestCodec;

@ReloadCodec(FetchRequestCodec.class)
public class FetchRequest extends Content {

	private final ResourceID resourceId;
	private final List<StoreKindSpecifier> specifiers;

	public FetchRequest(ResourceID resId, List<StoreKindSpecifier> specifiers) {
		resourceId = resId;
		this.specifiers = specifiers;
	}

	public ResourceID getResourceId() {
		return resourceId;
	}

	public List<StoreKindSpecifier> getSpecifiers() {
		return specifiers;
	}

	@Override
	public ContentType getType() {
		return ContentType.FETCH_REQ;
	}

	static class FetchRequestCodec extends Codec<FetchRequest> {

		private static final int SPECIFIERS_LENGTH_FIELD = U_INT16;

		private final Codec<ResourceID> resIdCodec;
		private final Codec<StoreKindSpecifier> dataSpecifierCodec;

		public FetchRequestCodec(ObjectGraph ctx) {
			super(ctx);
			resIdCodec = getCodec(ResourceID.class);
			dataSpecifierCodec = getCodec(StoreKindSpecifier.class);
		}

		@Override
		public void encode(FetchRequest obj, ByteBuf buf, Object... params) throws CodecException {
			resIdCodec.encode(obj.resourceId, buf);
			encodeSpecifiers(obj, buf);
		}

		private void encodeSpecifiers(FetchRequest obj, ByteBuf buf) throws com.github.reload.net.codecs.Codec.CodecException {
			Field lenFld = allocateField(buf, SPECIFIERS_LENGTH_FIELD);

			for (StoreKindSpecifier s : obj.specifiers) {
				dataSpecifierCodec.encode(s, buf);
			}

			lenFld.updateDataLength();
		}

		@Override
		public FetchRequest decode(ByteBuf buf, Object... params) throws CodecException {
			ResourceID resourceId = resIdCodec.decode(buf);
			List<StoreKindSpecifier> specifiers = decodeSpecifiers(buf);
			return new FetchRequest(resourceId, specifiers);
		}

		private List<StoreKindSpecifier> decodeSpecifiers(ByteBuf buf) throws com.github.reload.net.codecs.Codec.CodecException {
			List<StoreKindSpecifier> out = new ArrayList<StoreKindSpecifier>();

			ByteBuf specifiersData = readField(buf, SPECIFIERS_LENGTH_FIELD);
			while (specifiersData.readableBytes() > 0) {
				out.add(dataSpecifierCodec.decode(specifiersData));
			}
			specifiersData.release();

			return out;
		}

	}
}
