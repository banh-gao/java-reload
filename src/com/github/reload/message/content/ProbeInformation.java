package com.github.reload.message.content;

import io.netty.buffer.ByteBuf;
import com.github.reload.Context;
import com.github.reload.message.content.ProbeInformation.ProbeInformationCodec;
import com.github.reload.message.content.ProbeRequest.ProbeInformationType;
import com.github.reload.net.data.Codec;
import com.github.reload.net.data.ReloadCodec;

@ReloadCodec(ProbeInformationCodec.class)
public abstract class ProbeInformation {

	protected abstract ProbeInformationType getType();

	public static class ProbeInformationCodec extends Codec<ProbeInformation> {

		private static final int INFORMATION_LENGTH_FIELD = U_INT8;

		public ProbeInformationCodec(Context context) {
			super(context);
		}

		@Override
		public void encode(ProbeInformation obj, ByteBuf buf, Object... params) throws CodecException {
			ProbeInformationType type = obj.getType();

			buf.writeByte(type.getCode());
			Field lenFld = allocateField(buf, INFORMATION_LENGTH_FIELD);

			@SuppressWarnings("unchecked")
			Codec<ProbeInformation> codec = (Codec<ProbeInformation>) getCodec(type.getInfoClass());
			codec.encode(obj, buf);

			lenFld.updateDataLength();
		}

		@Override
		public ProbeInformation decode(ByteBuf buf, Object... params) throws CodecException {
			byte typeV = buf.readByte();
			ProbeInformationType type = ProbeInformationType.valueOf(typeV);

			if (type == null)
				throw new CodecException("Unknown probe information type " + typeV);

			ByteBuf infoData = readField(buf, INFORMATION_LENGTH_FIELD);

			@SuppressWarnings("unchecked")
			Codec<ProbeInformation> codec = (Codec<ProbeInformation>) getCodec(type.getInfoClass());

			return codec.decode(infoData);
		}

	}
}