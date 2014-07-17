package com.github.reload.net.encoders.content.storage;

import io.netty.buffer.ByteBuf;
import com.github.reload.Configuration;
import com.github.reload.net.encoders.Codec;
import com.github.reload.net.encoders.Codec.ReloadCodec;
import com.github.reload.net.encoders.content.storage.DictionaryMetadata.DictionaryMetadataCodec;
import com.github.reload.net.encoders.content.storage.DictionaryValue.Key;
import com.github.reload.storage.DataModel;
import com.github.reload.storage.DataModel.Metadata;

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
