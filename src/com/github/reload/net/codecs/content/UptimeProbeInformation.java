package com.github.reload.net.codecs.content;

import io.netty.buffer.ByteBuf;
import dagger.ObjectGraph;
import com.github.reload.net.codecs.Codec;
import com.github.reload.net.codecs.Codec.ReloadCodec;
import com.github.reload.net.codecs.content.ProbeRequest.ProbeInformationType;
import com.github.reload.net.codecs.content.UptimeProbeInformation.UptimeCodec;

/**
 * Probe information that reports the number of seconds this peer is online
 * 
 */
@ReloadCodec(UptimeCodec.class)
public class UptimeProbeInformation extends ProbeInformation {

	private final long uptime;

	public UptimeProbeInformation(long uptime) {
		this.uptime = uptime;
	}

	@Override
	public ProbeInformationType getType() {
		return ProbeInformationType.UPTIME;
	}

	/**
	 * @return The peer uptime in seconds
	 */
	public long getUptime() {
		return uptime;
	}

	@Override
	public String toString() {
		return "UptimeProbeInformation [uptime=" + uptime + "]";
	}

	static class UptimeCodec extends Codec<UptimeProbeInformation> {

		public UptimeCodec(ObjectGraph ctx) {
			super(ctx);
		}

		@Override
		public void encode(UptimeProbeInformation obj, ByteBuf buf, Object... params) throws CodecException {
			buf.writeInt((int) obj.uptime);
		}

		@Override
		public UptimeProbeInformation decode(ByteBuf buf, Object... params) throws CodecException {
			return new UptimeProbeInformation(buf.readUnsignedInt());
		}

	}

}
