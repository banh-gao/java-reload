package com.github.reload.storage.net;

import io.netty.buffer.ByteBuf;
import java.util.List;
import com.github.reload.Context;
import com.github.reload.message.ResourceID;
import com.github.reload.net.data.ReloadCodec;
import com.github.reload.storage.data.StoredDataSpecifier;
import com.github.reload.storage.net.StatRequest.StatRequestCodec;

@ReloadCodec(StatRequestCodec.class)
public class StatRequest extends FetchRequest {

	public StatRequest(ResourceID resId, List<StoredDataSpecifier> specifiers) {
		super(resId, specifiers);
	}

	public static class StatRequestCodec extends FetchRequestCodec {

		public StatRequestCodec(Context context) {
			super(context);
		}

		@Override
		public FetchRequest decode(ByteBuf buf, Object... params) throws com.github.reload.net.data.Codec.CodecException {
			FetchRequest req = super.decode(buf, params);
			return new StatRequest(req.getResourceId(), req.getSpecifiers());
		}

	}

}
