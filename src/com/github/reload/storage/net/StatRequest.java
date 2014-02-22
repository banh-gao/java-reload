package com.github.reload.storage;

import io.netty.buffer.ByteBuf;
import java.util.Arrays;
import java.util.List;
import com.github.reload.Context;
import com.github.reload.message.ContentType;
import com.github.reload.message.ResourceID;
import com.github.reload.net.data.ReloadCodec;
import com.github.reload.storage.StatRequest.StatRequestCodec;

@ReloadCodec(StatRequestCodec.class)
public class StatRequest extends QueryRequest {

	public StatRequest(ResourceID id, StoredDataSpecifier... specifiers) {
		this(id, Arrays.asList(specifiers));
	}

	public StatRequest(ResourceID id, List<StoredDataSpecifier> specifiers) {
		super(id, specifiers);
	}

	@Override
	public ContentType getType() {
		return ContentType.STAT_REQ;
	}

	public static class StatRequestCodec extends QueryRequestCodec {

		public StatRequestCodec(Context context) {
			super(context);
		}

		@Override
		protected QueryRequest implDecode(ResourceID resourceId, List<StoredDataSpecifier> specifiers, ByteBuf buf) {
			return new StatRequest(resourceId, specifiers);
		}
	}

}
