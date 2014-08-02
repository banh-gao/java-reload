package com.github.reload.services.storage.encoders;

import io.netty.buffer.ByteBuf;
import java.math.BigInteger;
import com.github.reload.components.ComponentsContext;
import com.github.reload.net.encoders.Codec;
import com.github.reload.net.encoders.Codec.ReloadCodec;
import com.github.reload.net.encoders.secBlock.Signature;
import com.github.reload.services.storage.DataModel;
import com.github.reload.services.storage.DataModel.DataValue;
import com.github.reload.services.storage.DataModel.Metadata;
import com.github.reload.services.storage.encoders.StoredMetadata.StoredMetadataCodec;

@ReloadCodec(StoredMetadataCodec.class)
public class StoredMetadata extends StoredData {

	public StoredMetadata(BigInteger storageTime, long lifetime, Metadata<? extends DataValue> value) {
		super(storageTime, lifetime, value, Signature.EMPTY_SIGNATURE);
	}

	static class StoredMetadataCodec extends Codec<StoredMetadata> {

		private static final int STORAGE_TIME_FIELD = U_INT64;
		private static final int DATA_LENGTH_FIELD = U_INT32;

		public StoredMetadataCodec(ComponentsContext ctx) {
			super(ctx);
		}

		@SuppressWarnings("unchecked")
		@Override
		public void encode(StoredMetadata obj, ByteBuf buf, Object... params) throws com.github.reload.net.encoders.Codec.CodecException {
			Field lenFld = allocateField(buf, DATA_LENGTH_FIELD);

			byte[] storTimeBytes = obj.getStorageTime().toByteArray();
			// Make sure the field has always a fixed size by padding with zeros
			buf.writeZero(STORAGE_TIME_FIELD - storTimeBytes.length);
			buf.writeBytes(storTimeBytes);

			buf.writeInt((int) obj.getLifeTime());

			Codec<Metadata<? extends DataValue>> valueCodec = (Codec<Metadata<? extends DataValue>>) getCodec(obj.getValue().getClass());

			valueCodec.encode((Metadata<? extends DataValue>) obj.getValue(), buf);

			lenFld.updateDataLength();
		}

		@Override
		public StoredMetadata decode(ByteBuf buf, Object... params) throws com.github.reload.net.encoders.Codec.CodecException {
			if (params.length < 1 || !(params[0] instanceof DataModel))
				throw new IllegalArgumentException("Data type needed to decode a stored data");

			ByteBuf dataFld = readField(buf, DATA_LENGTH_FIELD);

			byte[] storageTimeRaw = new byte[STORAGE_TIME_FIELD];
			dataFld.readBytes(storageTimeRaw);
			BigInteger storageTime = new BigInteger(1, storageTimeRaw);

			long lifeTime = dataFld.readUnsignedInt();

			@SuppressWarnings("unchecked")
			Codec<Metadata<? extends DataValue>> valueCodec = (Codec<Metadata<? extends DataValue>>) getCodec(((DataModel<?>) params[0]).getMetadataClass());

			Metadata<? extends DataValue> value = valueCodec.decode(dataFld);

			return new StoredMetadata(storageTime, lifeTime, value);
		}

	}
}
