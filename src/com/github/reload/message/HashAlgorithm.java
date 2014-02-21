package com.github.reload.message;

import io.netty.buffer.ByteBuf;
import java.util.EnumSet;
import com.github.reload.Context;
import com.github.reload.message.HashAlgorithm.HashAlgorithmCodec;
import com.github.reload.net.data.Codec;
import com.github.reload.net.data.ReloadCodec;

@ReloadCodec(HashAlgorithmCodec.class)
public enum HashAlgorithm {
	NONE((byte) 0x00, "NONE"),
	MD5((byte) 0x01, "MD5"),
	SHA1((byte) 0x02, "SHA1"),
	SHA224((byte) 0x03, "SHA-224"),
	SHA256((byte) 0x04, "SHA-256"),
	SHA384((byte) 0x05, "SHA-384"),
	SHA512((byte) 0x06, "SHA-512");

	private final byte code;
	private final String asString;

	private HashAlgorithm(byte code, String asString) {
		this.code = code;
		this.asString = asString;
	}

	public byte getCode() {
		return code;
	}

	public static HashAlgorithm valueOf(short code) {
		for (HashAlgorithm a : EnumSet.allOf(HashAlgorithm.class))
			if (a.code == code)
				return a;
		return null;
	}

	public static HashAlgorithm getFromString(String v) {
		for (HashAlgorithm a : EnumSet.allOf(HashAlgorithm.class))
			if (a.asString.equalsIgnoreCase(v))
				return a;
		return null;
	}

	public static class HashAlgorithmCodec extends Codec<HashAlgorithm> {

		public HashAlgorithmCodec(Context context) {
			super(context);
		}

		@Override
		public void encode(HashAlgorithm obj, ByteBuf buf, Object... params) throws CodecException {
			buf.writeByte(obj.code);
		}

		@Override
		public HashAlgorithm decode(ByteBuf buf, Object... params) throws CodecException {
			HashAlgorithm hashAlg = HashAlgorithm.valueOf(buf.readByte());
			if (hashAlg == null)
				throw new CodecException("Unsupported hash algorithm");

			return hashAlg;
		}
	}

	@Override
	public String toString() {
		return asString;
	}
}