package com.github.reload.services.storage.encoders;

import io.netty.buffer.ByteBuf;
import java.util.Objects;
import com.github.reload.components.ComponentsContext;
import com.github.reload.net.encoders.Codec;
import com.github.reload.net.encoders.Codec.ReloadCodec;
import com.github.reload.services.storage.encoders.ArrayMetadata.ArrayMetadataCodec;
import com.github.reload.services.storage.encoders.ArrayModel.ArrayValueSpecifier;
import com.github.reload.services.storage.encoders.DataModel.Metadata;
import com.github.reload.services.storage.encoders.DataModel.ValueSpecifier;

/**
 * Metadata of a stored array entry
 * 
 */
@ReloadCodec(ArrayMetadataCodec.class)
public class ArrayMetadata implements Metadata<ArrayValue> {

	private final long index;
	private final SingleMetadata singleMeta;

	public ArrayMetadata(long index, SingleMetadata singleMeta) {
		this.index = index;
		this.singleMeta = singleMeta;
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

		public ArrayMetadataCodec(ComponentsContext ctx) {
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
			return new ArrayMetadata(index, single);
		}

	}

	@Override
	public long getSize() {
		return singleMeta.getSize();
	}

	@Override
	public ValueSpecifier getMatchingSpecifier() {
		return new ArrayValueSpecifier().addRange(index, index);
	}
}
