package com.github.reload.net.encoders.content.storage;

import io.netty.buffer.ByteBuf;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Objects;
import com.github.reload.Configuration;
import com.github.reload.net.encoders.Codec;
import com.github.reload.net.encoders.Codec.ReloadCodec;
import com.github.reload.net.encoders.content.storage.SingleMetadata.SingleMetadataCodec;
import com.github.reload.net.encoders.secBlock.HashAlgorithm;
import com.github.reload.storage.DataModel.Metadata;

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

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), exists, storedValueSize, hashAlgorithm);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SingleMetadata other = (SingleMetadata) obj;
		if (exists != other.exists)
			return false;
		if (hashAlgorithm != other.hashAlgorithm)
			return false;
		if (!Arrays.equals(hashValue, other.hashValue))
			return false;
		if (storedValueSize != other.storedValueSize)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "SingleMetadata [exists=" + exists + ", storedValueSize=" + storedValueSize + ", hashAlgorithm=" + hashAlgorithm + ", hashValue=" + Arrays.toString(hashValue) + "]";
	}

	static class SingleMetadataCodec extends Codec<SingleMetadata> {

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
			hashFld.readBytes(hashValue);

			hashFld.release();

			return new SingleMetadata(exists, storedValueSize, hashAlgorithm, hashValue);
		}

	}

}
