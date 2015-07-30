package com.github.reload.services.storage.local;

import io.netty.buffer.ByteBuf;
import java.math.BigInteger;
import dagger.ObjectGraph;
import com.github.reload.net.codecs.Codec;
import com.github.reload.net.codecs.Codec.ReloadCodec;
import com.github.reload.net.codecs.secBlock.Signature;
import com.github.reload.services.storage.DataModel;
import com.github.reload.services.storage.DataModel.Metadata;
import com.github.reload.services.storage.local.StoredMetadata.StoredMetadataCodec;

@ReloadCodec(StoredMetadataCodec.class)
public class StoredMetadata extends StoredData {

	public StoredMetadata(BigInteger storageTime, long lifetime, Metadata value) {
		super(storageTime, lifetime, value, Signature.EMPTY_SIGNATURE);
	}

	static class StoredMetadataCodec extends Codec<StoredMetadata> {

		private static final int STORAGE_TIME_FIELD = U_INT64;
		private static final int DATA_LENGTH_FIELD = U_INT32;

		public StoredMetadataCodec(ObjectGraph ctx) {
			super(ctx);
		}

		@SuppressWarnings("unchecked")
		@Override
		public void encode(StoredMetadata obj, ByteBuf buf, Object... params) throws com.github.reload.net.codecs.Codec.CodecException {
			Field lenFld = allocateField(buf, DATA_LENGTH_FIELD);

			byte[] storTimeBytes = toUnsigned(obj.getStorageTime());
			// Make sure the field has always a fixed size by padding with zeros
			buf.writeZero(STORAGE_TIME_FIELD - storTimeBytes.length);
			buf.writeBytes(storTimeBytes);

			buf.writeInt((int) obj.getLifeTime());

			Codec<Metadata> valueCodec = (Codec<Metadata>) getCodec(obj.getValue().getClass());

			valueCodec.encode((Metadata) obj.getValue(), buf);

			lenFld.updateDataLength();
		}

		@Override
		public StoredMetadata decode(ByteBuf buf, Object... params) throws com.github.reload.net.codecs.Codec.CodecException {
			if (params.length < 1 || !(params[0] instanceof DataModel))
				throw new IllegalArgumentException("Data type needed to decode a stored data");

			ByteBuf dataFld = readField(buf, DATA_LENGTH_FIELD);

			byte[] storageTimeRaw = new byte[STORAGE_TIME_FIELD];
			dataFld.readBytes(storageTimeRaw);
			BigInteger storageTime = new BigInteger(1, storageTimeRaw);

			long lifeTime = dataFld.readUnsignedInt();

			@SuppressWarnings("unchecked")
			Codec<Metadata> valueCodec = (Codec<Metadata>) getCodec(((DataModel) params[0]).getMetadataClass());

			Metadata value = valueCodec.decode(dataFld);

			return new StoredMetadata(storageTime, lifeTime, value);
		}

	}
}
