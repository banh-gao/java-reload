package com.github.reload.net.encoders.content;

import io.netty.buffer.ByteBuf;
import com.github.reload.Configuration;
import com.github.reload.net.encoders.Codec;
import com.github.reload.net.encoders.Codec.ReloadCodec;
import com.github.reload.net.encoders.content.ProbeRequest.ProbeInformationType;
import com.github.reload.net.encoders.content.ResponsibleSetProbeInformation.RespSetCodec;

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

		public RespSetCodec(Configuration conf) {
			super(conf);
		}

		@Override
		public void encode(ResponsibleSetProbeInformation obj, ByteBuf buf, Object... params) throws CodecException {
			buf.writeInt((int) obj.responsiblePpb);
		}

		@Override
		public ResponsibleSetProbeInformation decode(ByteBuf buf, Object... params) throws CodecException {
			return new ResponsibleSetProbeInformation(buf.readUnsignedInt());
		}

	}

}
