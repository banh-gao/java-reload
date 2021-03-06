package com.github.reload.net.ice;

import io.netty.buffer.ByteBuf;
import dagger.ObjectGraph;
import com.github.reload.net.codecs.Codec;
import com.github.reload.net.codecs.Codec.ReloadCodec;
import com.github.reload.net.ice.IceExtension.IceExtensionCodec;

@ReloadCodec(IceExtensionCodec.class)
public class IceExtension {

	private final byte[] name;
	private final byte[] value;

	public IceExtension(byte[] name, byte[] value) {
		super();
		this.name = name;
		this.value = value;
	}

	static class IceExtensionCodec extends Codec<IceExtension> {

		private static final int NAME_LENGTH_FIELD = U_INT16;
		private static final int VALUE_LENGTH_FIELD = U_INT16;

		public IceExtensionCodec(ObjectGraph ctx) {
			super(ctx);
		}

		@Override
		public void encode(IceExtension obj, ByteBuf buf, Object... params) throws com.github.reload.net.codecs.Codec.CodecException {
			Field nameLenFld = allocateField(buf, NAME_LENGTH_FIELD);
			buf.writeBytes(obj.name);
			nameLenFld.updateDataLength();

			Field valLenFld = allocateField(buf, VALUE_LENGTH_FIELD);
			buf.writeBytes(obj.value);
			valLenFld.updateDataLength();
		}

		@Override
		public IceExtension decode(ByteBuf buf, Object... params) throws com.github.reload.net.codecs.Codec.CodecException {
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
