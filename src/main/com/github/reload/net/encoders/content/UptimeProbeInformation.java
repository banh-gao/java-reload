package com.github.reload.net.encoders.content;

import io.netty.buffer.ByteBuf;
import com.github.reload.conf.Configuration;
import com.github.reload.net.encoders.Codec;
import com.github.reload.net.encoders.Codec.ReloadCodec;
import com.github.reload.net.encoders.content.ProbeRequest.ProbeInformationType;
import com.github.reload.net.encoders.content.UptimeProbeInformation.UptimeCodec;

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

		public UptimeCodec(Configuration conf) {
			super(conf);
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
