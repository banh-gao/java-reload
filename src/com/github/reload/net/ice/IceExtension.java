package com.github.reload.net.ice;

import io.netty.buffer.ByteBuf;
import com.github.reload.Context;
import com.github.reload.net.data.Codec;
import com.github.reload.net.data.ReloadCodec;

@ReloadCodec(IceCandidateCodec.class)
public class IceExtension {

	private final byte[] name;
	private final byte[] value;

	public IceExtension(byte[] name, byte[] value) {
		super();
		this.name = name;
		this.value = value;
	}

	public static class IceExtensionCodec extends Codec<IceExtension> {

		private static final int NAME_LENGTH_FIELD = U_INT16;
		private static final int VALUE_LENGTH_FIELD = U_INT16;

		public IceExtensionCodec(Context context) {
			super(context);
		}

		@Override
		public void encode(IceExtension obj, ByteBuf buf) throws com.github.reload.net.data.Codec.CodecException {
			Field nameLenFld = allocateField(buf, NAME_LENGTH_FIELD);
			buf.writeBytes(obj.name);
			nameLenFld.updateDataLength();

			Field valLenFld = allocateField(buf, VALUE_LENGTH_FIELD);
			buf.writeBytes(obj.value);
			valLenFld.updateDataLength();
		}

		@Override
		public IceExtension decode(ByteBuf buf) throws com.github.reload.net.data.Codec.CodecException {
			ByteBuf nameBuf = readField(buf, NAME_LENGTH_FIELD);
			byte[] name = new byte[nameBuf.readableBytes()];
			nameBuf.readBytes(name);

			ByteBuf valueBuf = readField(buf, VALUE_LENGTH_FIELD);
			byte[] value = new byte[valueBuf.readableBytes()];
			valueBuf.readBytes(value);

			return new IceExtension(name, value);
		}

	}
}
