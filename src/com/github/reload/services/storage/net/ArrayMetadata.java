package com.github.reload.services.storage.net;

import io.netty.buffer.ByteBuf;
import java.util.Objects;
import dagger.ObjectGraph;
import com.github.reload.net.codecs.Codec;
import com.github.reload.net.codecs.Codec.ReloadCodec;
import com.github.reload.net.codecs.secBlock.HashAlgorithm;
import com.github.reload.services.storage.DataModel.DataValue;
import com.github.reload.services.storage.DataModel.Metadata;
import com.github.reload.services.storage.DataModel.ValueSpecifier;
import com.github.reload.services.storage.net.ArrayMetadata.ArrayMetadataCodec;

/**
 * Metadata of a stored array entry
 * 
 */
@ReloadCodec(ArrayMetadataCodec.class)
public class ArrayMetadata implements Metadata {

	private long index;
	private SingleMetadata singleMeta;

	@Override
	public void setMetadata(DataValue v, HashAlgorithm hashAlg) {
		ArrayValue value = (ArrayValue) v;
		index = value.getIndex();

		singleMeta = new SingleMetadata();
		singleMeta.setMetadata(v, hashAlg);
	}

	public long getIndex() {
		return index;
	}

	public SingleMetadata getSingleMeta() {
		return singleMeta;
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), index, singleMeta);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ArrayMetadata other = (ArrayMetadata) obj;
		if (index != other.index)
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
		return "ArrayMetadata [index=" + index + ", singleMeta=" + singleMeta + "]";
	}

	static class ArrayMetadataCodec extends Codec<ArrayMetadata> {

		private final Codec<SingleMetadata> singleCodec;

		public ArrayMetadataCodec(ObjectGraph ctx) {
			super(ctx);
			singleCodec = getCodec(SingleMetadata.class);
		}

		@Override
		public void encode(ArrayMetadata obj, ByteBuf buf, Object... params) throws CodecException {
			buf.writeInt((int) obj.index);
			singleCodec.encode(obj.singleMeta, buf);
		}

		@Override
		public ArrayMetadata decode(ByteBuf buf, Object... params) throws CodecException {
			long index = buf.readUnsignedInt();
			SingleMetadata single = singleCodec.decode(buf);

			ArrayMetadata m = new ArrayMetadata();
			m.index = index;
			m.singleMeta = single;

			return new ArrayMetadata();
		}

	}

	@Override
	public long getSize() {
		return singleMeta.getSize();
	}

	@Override
	public ValueSpecifier getMatchingSpecifier() {
		throw new AssertionError("Metadata don't have specifiers");
	}
}
