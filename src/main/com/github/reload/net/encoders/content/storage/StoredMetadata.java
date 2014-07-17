package com.github.reload.net.encoders.content.storage;

import io.netty.buffer.ByteBuf;
import java.math.BigInteger;
import com.github.reload.Configuration;
import com.github.reload.net.encoders.Codec;
import com.github.reload.net.encoders.Codec.ReloadCodec;
import com.github.reload.net.encoders.content.storage.StoredMetadata.StoredMetadataCodec;
import com.github.reload.net.encoders.secBlock.Signature;
import com.github.reload.storage.DataModel;
import com.github.reload.storage.DataModel.DataValue;
import com.github.reload.storage.DataModel.Metadata;

@ReloadCodec(StoredMetadataCodec.class)
public class StoredMetadata extends StoredData {

	public StoredMetadata(BigInteger storageTime, long lifetime, Metadata<? extends DataValue> value) {
		super(storageTime, lifetime, value, Signature.EMPTY_SIGNATURE);
	}

	@SuppressWarnings("unchecked")
	@Override
	public Metadata<? extends DataValue> getValue() {
		return (Metadata<? extends DataValue>) super.getValue();
	}

	public static class StoredMetadataCodec extends Codec<StoredMetadata> {

		private static final int DATA_LENGTH_FIELD = U_INT32;

		public StoredMetadataCodec(Configuration conf) {
			super(conf);
		}

		@Override
		public StoredMetadata decode(ByteBuf buf, Object... params) throws com.github.reload.net.encoders.Codec.CodecException {
			if (params.length < 1 || !(params[0] instanceof DataModel))
				throw new IllegalArgumentException("Data type needed to decode a stored data");

			ByteBuf dataFld = readField(buf, DATA_LENGTH_FIELD);

			byte[] storageTimeRaw = new byte[8];
			dataFld.readBytes(storageTimeRaw);
			BigInteger storageTime = new BigInteger(1, storageTimeRaw);

			long lifeTime = dataFld.readUnsignedInt();

			@SuppressWarnings("unchecked")
			Codec<Metadata<? extends DataValue>> valueCodec = (Codec<Metadata<? extends DataValue>>) getCodec(((DataModel<?>) params[0]).getMetadataClass());

			Metadata<? extends DataValue> value = valueCodec.decode(dataFld);

			return new StoredMetadata(storageTime, lifeTime, value);
		}

		@Override
		public void encode(StoredMetadata obj, ByteBuf buf, Object... params) throws com.github.reload.net.encoders.Codec.CodecException {
			Field lenFld = allocateField(buf, DATA_LENGTH_FIELD);

			buf.writeBytes(obj.getStorageTime().toByteArray());
			buf.writeInt((int) obj.getLifeTime());

			@SuppressWarnings("unchecked")
			Codec<Metadata<? extends DataValue>> valueCodec = (Codec<Metadata<? extends DataValue>>) getCodec(obj.getValue().getClass());

			valueCodec.encode(obj.getValue(), buf);

			lenFld.updateDataLength();
		}

	}
}
