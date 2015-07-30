package com.github.reload.services.storage.net;

import io.netty.buffer.ByteBuf;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Objects;
import dagger.ObjectGraph;
import com.github.reload.net.codecs.Codec;
import com.github.reload.net.codecs.Codec.ReloadCodec;
import com.github.reload.net.codecs.secBlock.HashAlgorithm;
import com.github.reload.services.storage.DataModel.DataValue;
import com.github.reload.services.storage.DataModel.Metadata;
import com.github.reload.services.storage.DataModel.ValueSpecifier;
import com.github.reload.services.storage.net.SingleMetadata.SingleMetadataCodec;

/**
 * Metadata used to describe a stored data
 * 
 */
@ReloadCodec(SingleMetadataCodec.class)
public class SingleMetadata implements Metadata {

	private boolean exists;
	private long storedValueSize;
	private HashAlgorithm hashAlgorithm;
	private byte[] hashValue;

	@Override
	public void setMetadata(DataValue v, HashAlgorithm hashAlg) {
		SingleValue value = (SingleValue) v;
		this.exists = value.exists();
		this.storedValueSize = value.getSize();
		if (storedValueSize == 0) {
			hashAlgorithm = HashAlgorithm.NONE;
			this.hashValue = new byte[0];
		} else {
			hashAlgorithm = hashAlg;
			this.hashValue = computeHash(hashAlg, value.getValue());
		}
	}

	static byte[] computeHash(HashAlgorithm hashAlgorithm, byte[] value) {
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

		public SingleMetadataCodec(ObjectGraph ctx) {
			super(ctx);
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

			SingleMetadata m = new SingleMetadata();

			m.exists = exists;
			m.storedValueSize = storedValueSize;
			m.hashAlgorithm = hashAlgorithm;
			m.hashValue = hashValue;

			return m;
		}

	}

	@Override
	public long getSize() {
		return storedValueSize;
	}

	@Override
	public ValueSpecifier getMatchingSpecifier() {
		throw new AssertionError("Metadata don't have specifiers");
	}
}
