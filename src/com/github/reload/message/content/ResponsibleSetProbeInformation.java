package com.github.reload.message.content;

import io.netty.buffer.ByteBuf;
import com.github.reload.Context;
import com.github.reload.message.content.ProbeRequest.ProbeInformationType;
import com.github.reload.message.content.ResponsibleSetProbeInformation.RespSetCodec;
import com.github.reload.net.data.Codec;
import com.github.reload.net.data.ReloadCodec;

/**
 * Probe information that reports the magnitude of the resource space this peer
 * is responsible for in part per billion over the total resource keyspace
 * 
 */
@ReloadCodec(RespSetCodec.class)
public class ResponsibleSetProbeInformation extends ProbeInformation {

	private final long responsiblePpb;

	public ResponsibleSetProbeInformation(long responsiblePartPerBillion) {
		responsiblePpb = responsiblePartPerBillion;
	}

	@Override
	public ProbeInformationType getType() {
		return ProbeInformationType.RESPONSIBLE_SET;
	}

	public long getResponsiblePartPerBillion() {
		return responsiblePpb;
	}

	@Override
	public String toString() {
		return "ResponsibleSetProbeInformation [responsiblePpb=" + responsiblePpb + "]";
	}

	public static class RespSetCodec extends Codec<ResponsibleSetProbeInformation> {

		public RespSetCodec(Context context) {
			super(context);
		}

		@Override
		public void encode(ResponsibleSetProbeInformation obj, ByteBuf buf) throws CodecException {
			buf.writeInt((int) obj.responsiblePpb);
		}

		@Override
		public ResponsibleSetProbeInformation decode(ByteBuf buf) throws CodecException {
			return new ResponsibleSetProbeInformation(buf.readUnsignedInt());
		}

	}

}
