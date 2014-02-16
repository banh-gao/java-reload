package com.github.reload.storage;

import io.netty.buffer.ByteBuf;
import java.util.List;
import com.github.reload.Context;
import com.github.reload.message.ContentType;
import com.github.reload.message.ResourceID;
import com.github.reload.net.data.ReloadCodec;
import com.github.reload.storage.FetchRequest.FetchRequestCodec;

@ReloadCodec(FetchRequestCodec.class)
public class FetchRequest extends QueryRequest {

	public FetchRequest(ResourceID id, List<DataSpecifier> specifiers) {
		super(id, specifiers);
	}

	@Override
	public ContentType getType() {
		return ContentType.FETCH_REQ;
	}

	public static class FetchRequestCodec extends QueryRequestCodec {

		public FetchRequestCodec(Context context) {
			super(context);
		}

		@Override
		protected QueryRequest implDecode(ResourceID resourceId, List<DataSpecifier> specifiers, ByteBuf buf) {
			return new FetchRequest(resourceId, specifiers);
		}
	}
}
