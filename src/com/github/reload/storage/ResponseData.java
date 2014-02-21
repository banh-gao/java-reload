package com.github.reload.storage;

import io.netty.buffer.ByteBuf;
import java.math.BigInteger;
import com.github.reload.Context;
import com.github.reload.message.SignerIdentity;
import com.github.reload.net.data.Codec;

/**
 * An answer data to a storage query
 * 
 */
public abstract class ResponseData {

	private DataKind kind;
	private BigInteger storageTime;
	private long lifeTime;

	public DataKind getKind() {
		return kind;
	}

	public long getLifeTime() {
		return lifeTime;
	}

	public BigInteger getStorageTime() {
		return storageTime;
	}

	protected abstract SignerIdentity getSignerIdentity();

	public static abstract class ResponseDataCodec extends Codec<ResponseData> {

		private static final int DATA_LENGTH_FIELD = U_INT32;

		public ResponseDataCodec(Context context) {
			super(context);
		}

		@Override
		public void encode(ResponseData obj, ByteBuf buf) throws CodecException {
			ByteBuf dataFld = readField(buf, DATA_LENGTH_FIELD);

			byte[] storageTimeData = new byte[8];
			dataFld.readBytes(storageTimeData);
			BigInteger storageTime = new BigInteger(1, storageTimeData);

			long lifeTime = dataFld.readUnsignedInt();

			encodeValue(obj, buf);
		}

		protected abstract void encodeValue(ResponseData obj, ByteBuf buf) throws CodecException;

		@Override
		public ResponseData decode(ByteBuf buf) throws CodecException {
			ByteBuf dataFld = readField(buf, DATA_LENGTH_FIELD);

			byte[] storageTimeData = new byte[8];
			dataFld.readBytes(storageTimeData);
			BigInteger storageTime = new BigInteger(1, storageTimeData);

			long lifeTime = dataFld.readUnsignedInt();

			return decodeValue(storageTime, lifeTime, buf);
		}

		protected abstract ResponseData decodeValue(BigInteger storageTime, long lifeTime, ByteBuf buf) throws CodecException;
	}
}