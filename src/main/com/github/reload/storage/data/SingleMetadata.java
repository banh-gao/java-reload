package com.github.reload.storage.data;

import io.netty.buffer.ByteBuf;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import com.github.reload.Configuration;
import com.github.reload.net.encoders.Codec;
import com.github.reload.net.encoders.Codec.ReloadCodec;
import com.github.reload.net.encoders.secBlock.HashAlgorithm;
import com.github.reload.storage.data.DataModel.Metadata;
import com.github.reload.storage.data.SingleMetadata.SingleMetadataCodec;

/**
 * Metadata used to describe a stored data
 * 
 */
@ReloadCodec(SingleMetadataCodec.class)
public class SingleMetadata implements Metadata<SingleValue> {

	private final boolean exists;
	private final long storedValueSize;
	private final HashAlgorithm hashAlgorithm;

	private final byte[] hashValue;

	public SingleMetadata(boolean exists, long valueSize, HashAlgorithm hashAlg, byte[] hashValue) {
		this.exists = exists;
		storedValueSize = valueSize;
		if (storedValueSize == 0) {
			hashAlgorithm = HashAlgorithm.NONE;
			this.hashValue = new byte[0];
		} else {
			hashAlgorithm = hashAlg;
			this.hashValue = hashValue;
		}
	}

	public static byte[] computeHash(HashAlgorithm hashAlgorithm, byte[] value) {
		MessageDigest digestor;
		try {
			digestor = MessageDigest.getInstance(hashAlgorithm.toString());
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
		return digestor.digest(value);
	}

	public boolean exists() {
		return exists;
	}

	public long getStoredValueSize() {
		return storedValueSize;
	}

	public HashAlgorithm getHashAlgorithm() {
		return hashAlgorithm;
	}

	public byte[] getHashValue() {
		return hashValue;
	}

	public static class SingleMetadataCodec extends Codec<SingleMetadata> {

		private static final int HASHVALUE_LENGTH_FIELD = U_INT8;

		private final Codec<HashAlgorithm> hashAlgCodec;

		public SingleMetadataCodec(Configuration conf) {
			super(conf);
			hashAlgCodec = getCodec(HashAlgorithm.class);
		}

		@Override
		public void encode(SingleMetadata obj, ByteBuf buf, Object... params) throws CodecException {
			buf.writeByte(obj.exists ? 1 : 0);
			buf.writeInt((int) obj.storedValueSize);
			hashAlgCodec.encode(obj.hashAlgorithm, buf);

			Field lenFld = allocateField(buf, HASHVALUE_LENGTH_FIELD);
			buf.writeBytes(obj.hashValue);
			lenFld.updateDataLength();
		}

		@Override
		public SingleMetadata decode(ByteBuf buf, Object... params) throws CodecException {

			boolean exists = (buf.readUnsignedByte() > 0);
			long storedValueSize = buf.readUnsignedInt();

			HashAlgorithm hashAlgorithm = hashAlgCodec.decode(buf);

			ByteBuf hashFld = readField(buf, HASHVALUE_LENGTH_FIELD);

			byte[] hashValue = new byte[hashFld.readableBytes()];
			buf.readBytes(hashValue);

			return new SingleMetadata(exists, storedValueSize, hashAlgorithm, hashValue);
		}

	}

}
