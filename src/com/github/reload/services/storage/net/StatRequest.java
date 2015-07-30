package com.github.reload.services.storage.net;

import io.netty.buffer.ByteBuf;
import java.util.List;
import dagger.ObjectGraph;
import com.github.reload.net.codecs.Codec.ReloadCodec;
import com.github.reload.net.codecs.content.ContentType;
import com.github.reload.net.codecs.header.ResourceID;
import com.github.reload.services.storage.net.StatRequest.StatRequestCodec;

@ReloadCodec(StatRequestCodec.class)
public class StatRequest extends FetchRequest {

	public StatRequest(ResourceID resId, List<StoreKindSpecifier> specifiers) {
		super(resId, specifiers);
	}

	@Override
	public ContentType getType() {
		return ContentType.STAT_REQ;
	}

	static class StatRequestCodec extends FetchRequestCodec {

		public StatRequestCodec(ObjectGraph ctx) {
			super(ctx);
		}

		@Override
		public FetchRequest decode(ByteBuf buf, Object... params) throws com.github.reload.net.codecs.Codec.CodecException {
			FetchRequest req = super.decode(buf, params);
			return new StatRequest(req.getResourceId(), req.getSpecifiers());
		}

	}

}
