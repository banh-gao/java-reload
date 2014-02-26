package com.github.reload.storage.data;

import io.netty.buffer.ByteBuf;
import com.github.reload.Configuration;
import com.github.reload.net.data.Codec;
import com.github.reload.net.data.ReloadCodec;
import com.github.reload.storage.data.DataModel.Metadata;
import com.github.reload.storage.data.DictionaryMetadata.DictionaryMetadataCodec;
import com.github.reload.storage.data.DictionaryValue.Key;

/**
 * Metadata of a stored dictionary entry
 * 
 */
@ReloadCodec(DictionaryMetadataCodec.class)
public class DictionaryMetadata implements Metadata<DictionaryValue> {

	private final Key key;
	private final SingleMetadata singleMeta;

	public DictionaryMetadata(Key key, SingleMetadata singleMeta) {
		this.key = key;
		this.singleMeta = singleMeta;
	}

	public Key getKey() {
		return key;
	}

	public static class DictionaryMetadataCodec extends Codec<DictionaryMetadata> {

		private final Codec<Key> keyCodec;
		private final Codec<SingleMetadata> singleCodec;

		public DictionaryMetadataCodec(Configuration conf) {
			super(conf);
			keyCodec = getCodec(Key.class);
			singleCodec = getCodec(SingleMetadata.class);
		}

		@Override
		public void encode(DictionaryMetadata obj, ByteBuf buf, Object... params) throws CodecException {
			keyCodec.encode(obj.key, buf);
			singleCodec.encode(obj.singleMeta, buf);
		}

		@Override
		public DictionaryMetadata decode(ByteBuf buf, Object... params) throws CodecException {
			Key key = keyCodec.decode(buf);
			SingleMetadata single = singleCodec.decode(buf);
			return new DictionaryMetadata(key, single);
		}

	}
}
