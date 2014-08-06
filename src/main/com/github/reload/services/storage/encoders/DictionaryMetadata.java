package com.github.reload.services.storage.encoders;

import io.netty.buffer.ByteBuf;
import java.util.Arrays;
import com.github.reload.components.ComponentsContext;
import com.github.reload.net.encoders.Codec;
import com.github.reload.net.encoders.Codec.ReloadCodec;
import com.github.reload.services.storage.encoders.DataModel.Metadata;
import com.github.reload.services.storage.encoders.DictionaryMetadata.DictionaryMetadataCodec;

/**
 * Metadata of a stored dictionary entry
 * 
 */
@ReloadCodec(DictionaryMetadataCodec.class)
public class DictionaryMetadata implements Metadata<DictionaryValue> {

	private final byte[] key;
	private final SingleMetadata singleMeta;

	public DictionaryMetadata(byte[] key, SingleMetadata singleMeta) {
		this.key = key;
		this.singleMeta = singleMeta;
	}

	public byte[] getKey() {
		return key;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(key);
		result = prime * result + ((singleMeta == null) ? 0 : singleMeta.hashCode());
		return result;
	}

	@Override
	public String toString() {
		return "DictionaryMetadata [key=" + key + ", singleMeta=" + singleMeta + "]";
	}

	static class DictionaryMetadataCodec extends Codec<DictionaryMetadata> {

		private final Codec<SingleMetadata> singleCodec;

		public DictionaryMetadataCodec(ComponentsContext ctx) {
			super(ctx);
			singleCodec = getCodec(SingleMetadata.class);
		}

		@Override
		public void encode(DictionaryMetadata obj, ByteBuf buf, Object... params) throws CodecException {
			DictionaryValue.DictionaryValueCodec.encodeKey(obj.key, buf);
			singleCodec.encode(obj.singleMeta, buf);
		}

		@Override
		public DictionaryMetadata decode(ByteBuf buf, Object... params) throws CodecException {
			byte[] key = DictionaryValue.DictionaryValueCodec.decodeKey(buf);
			SingleMetadata single = singleCodec.decode(buf);
			return new DictionaryMetadata(key, single);
		}

	}

	@Override
	public long getSize() {
		return singleMeta.getSize();
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
		if (!Arrays.equals(key, other.key))
			return false;
		if (singleMeta == null) {
			if (other.singleMeta != null)
				return false;
		} else if (!singleMeta.equals(other.singleMeta))
			return false;
		return true;
	}

}
