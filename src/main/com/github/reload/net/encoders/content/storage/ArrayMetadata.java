package com.github.reload.net.encoders.content.storage;

import io.netty.buffer.ByteBuf;
import com.github.reload.Configuration;
import com.github.reload.net.encoders.Codec;
import com.github.reload.net.encoders.Codec.ReloadCodec;
import com.github.reload.net.encoders.content.storage.ArrayMetadata.ArrayMetadataCodec;
import com.github.reload.storage.DataModel;
import com.github.reload.storage.DataModel.Metadata;

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

	public static class ArrayMetadataCodec extends Codec<ArrayMetadata> {

		private final Codec<SingleMetadata> singleCodec;

		public ArrayMetadataCodec(Configuration conf) {
			super(conf);
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
}
