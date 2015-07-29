package com.github.reload.services.storage.net;

import io.netty.buffer.ByteBuf;
import java.util.Arrays;
import com.github.reload.components.ComponentsContext;
import com.github.reload.net.encoders.Codec;
import com.github.reload.net.encoders.Codec.ReloadCodec;
import com.github.reload.net.encoders.secBlock.HashAlgorithm;
import com.github.reload.services.storage.DataModel.DataValue;
import com.github.reload.services.storage.DataModel.Metadata;
import com.github.reload.services.storage.DataModel.ValueSpecifier;
import com.github.reload.services.storage.net.DictionaryMetadata.DictionaryMetadataCodec;

/**
 * Metadata of a stored dictionary entry
 * 
 */
@ReloadCodec(DictionaryMetadataCodec.class)
public class DictionaryMetadata implements Metadata {

	private byte[] key;
	private SingleMetadata singleMeta;

	@Override
	public void setMetadata(DataValue v, HashAlgorithm hashAlg) {
		DictionaryValue value = (DictionaryValue) v;
		this.key = value.getKey();
		this.singleMeta = new SingleMetadata();
		this.singleMeta.setMetadata(value.getValue(), hashAlg);
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

			DictionaryMetadata m = new DictionaryMetadata();
			m.key = key;
			m.singleMeta = single;

			return m;
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

	@Override
	public ValueSpecifier getMatchingSpecifier() {
		throw new AssertionError("Metadata don't have specifiers");
	}

}
