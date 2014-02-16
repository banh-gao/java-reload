package com.github.reload.storage;

import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import com.github.reload.Context;
import com.github.reload.message.Content;
import com.github.reload.message.ResourceID;
import com.github.reload.net.data.Codec;

public abstract class QueryRequest extends Content {

	final ResourceID resourceId;
	final List<DataSpecifier> specifiers;

	public QueryRequest(ResourceID id, List<DataSpecifier> specifiers) {
		resourceId = id;
		this.specifiers = specifiers;
	}

	public ResourceID getResourceId() {
		return resourceId;
	}

	public List<DataSpecifier> getDataSpecifiers() {
		return specifiers;
	}

	public static abstract class QueryRequestCodec extends Codec<QueryRequest> {

		private static final int SPECIFIERS_LENGTH_FIELD = U_INT16;

		private final Codec<ResourceID> resIdCodec;
		private final Codec<DataSpecifier> dataSpecifierCodec;

		public QueryRequestCodec(Context context) {
			super(context);
			resIdCodec = getCodec(ResourceID.class);
			dataSpecifierCodec = getCodec(DataSpecifier.class);
		}

		@Override
		public void encode(QueryRequest obj, ByteBuf buf) throws CodecException {
			resIdCodec.encode(obj.resourceId, buf);
			writeSpecifiersTo(obj, buf);
		}

		private void writeSpecifiersTo(QueryRequest obj, ByteBuf buf) throws com.github.reload.net.data.Codec.CodecException {
			Field lenFld = allocateField(buf, SPECIFIERS_LENGTH_FIELD);

			for (DataSpecifier s : obj.specifiers) {
				dataSpecifierCodec.encode(s, buf);
			}

			lenFld.updateDataLength();
		}

		@Override
		public QueryRequest decode(ByteBuf buf) throws CodecException {
			ResourceID resourceId = resIdCodec.decode(buf);
			List<DataSpecifier> specifiers = readSpecifiers(buf);
			return implDecode(resourceId, specifiers, buf);
		}

		protected abstract QueryRequest implDecode(ResourceID resourceId, List<DataSpecifier> specifiers, ByteBuf buf);

		private List<DataSpecifier> readSpecifiers(ByteBuf buf) throws com.github.reload.net.data.Codec.CodecException {
			List<DataSpecifier> out = new ArrayList<DataSpecifier>();

			ByteBuf specifiersData = readField(buf, SPECIFIERS_LENGTH_FIELD);
			while (specifiersData.readableBytes() > 0) {
				out.add(dataSpecifierCodec.decode(specifiersData));
			}
			specifiersData.release();

			return out;
		}

	}
}
