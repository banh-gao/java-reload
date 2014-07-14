package com.github.reload.storage.net;

import io.netty.buffer.ByteBuf;
import java.util.List;
import com.github.reload.Configuration;
import com.github.reload.message.Codec.ReloadCodec;
import com.github.reload.message.ResourceID;
import com.github.reload.storage.data.StoredDataSpecifier;
import com.github.reload.storage.net.StatRequest.StatRequestCodec;

@ReloadCodec(StatRequestCodec.class)
public class StatRequest extends FetchRequest {

	public StatRequest(ResourceID resId, List<StoredDataSpecifier> specifiers) {
		super(resId, specifiers);
	}

	public static class StatRequestCodec extends FetchRequestCodec {

		public StatRequestCodec(Configuration conf) {
			super(conf);
		}

		@Override
		public FetchRequest decode(ByteBuf buf, Object... params) throws com.github.reload.message.Codec.CodecException {
			FetchRequest req = super.decode(buf, params);
			return new StatRequest(req.getResourceId(), req.getSpecifiers());
		}

	}

}
