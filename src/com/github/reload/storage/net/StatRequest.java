package com.github.reload.storage.net;

import java.util.Arrays;
import java.util.List;
import com.github.reload.Context;
import com.github.reload.message.Content;
import com.github.reload.message.ContentType;
import com.github.reload.message.ResourceID;
import com.github.reload.net.data.Codec;
import com.github.reload.net.data.ReloadCodec;
import com.github.reload.storage.data.StoredDataSpecifier;
import com.github.reload.storage.net.StatRequest.StatRequestCodec;

@ReloadCodec(StatRequestCodec.class)
public class StatRequest extends Content {

	public StatRequest(ResourceID id, StoredDataSpecifier... specifiers) {
		this(id, Arrays.asList(specifiers));
	}

	public StatRequest(ResourceID id, List<StoredDataSpecifier> specifiers) {

	}

	@Override
	public ContentType getType() {
		return ContentType.STAT_REQ;
	}

	public static class StatRequestCodec extends Codec<StatRequest> {

		public StatRequestCodec(Context context) {
			super(context);
		}
	}

}
