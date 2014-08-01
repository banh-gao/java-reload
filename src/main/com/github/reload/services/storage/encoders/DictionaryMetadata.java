package com.github.reload.services.storage.encoders;

import io.netty.buffer.ByteBuf;
import java.util.Objects;
import com.github.reload.conf.Configuration;
import com.github.reload.net.encoders.Codec;
import com.github.reload.net.encoders.Codec.ReloadCodec;
import com.github.reload.services.storage.DataModel.Metadata;
import com.github.reload.services.storage.encoders.DictionaryMetadata.DictionaryMetadataCodec;
import com.github.reload.services.storage.encoders.DictionaryValue.Key;

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

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), key, singleMeta);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DictionaryMetadata other = (DictionaryMetadata) obj;
		if (key == null) {
			if (other.key != null)
				return false;
		} else if (!key.equals(other.key))
			return false;
		if (singleMeta == null) {
			if (other.singleMeta != null)
				return false;
		} else if (!singleMeta.equals(other.singleMeta))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "DictionaryMetadata [key=" + key + ", singleMeta=" + singleMeta + "]";
	}

	static class DictionaryMetadataCodec extends Codec<DictionaryMetadata> {

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

	@Override
	public long getSize() {
		return singleMeta.getSize();
	}

}
