package com.github.reload.net.encoders.content.storage;

import io.netty.buffer.ByteBuf;
import java.util.List;
import com.github.reload.Configuration;
import com.github.reload.net.encoders.Codec.ReloadCodec;
import com.github.reload.net.encoders.content.ContentType;
import com.github.reload.net.encoders.content.storage.StatRequest.StatRequestCodec;
import com.github.reload.net.encoders.header.ResourceID;

@ReloadCodec(StatRequestCodec.class)
public class StatRequest extends FetchRequest {

	public StatRequest(ResourceID resId, List<StoredDataSpecifier> specifiers) {
		super(resId, specifiers);
	}

	@Override
	public ContentType getType() {
		return ContentType.STAT_REQ;
	}

	public static class StatRequestCodec extends FetchRequestCodec {

		public StatRequestCodec(Configuration conf) {
			super(conf);
		}

		@Override
		public FetchRequest decode(ByteBuf buf, Object... params) throws com.github.reload.net.encoders.Codec.CodecException {
			FetchRequest req = super.decode(buf, params);
			return new StatRequest(req.getResourceId(), req.getSpecifiers());
		}

	}

}
