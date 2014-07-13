package com.github.reload.message.content;

import io.netty.buffer.ByteBuf;
import com.github.reload.Configuration;
import com.github.reload.message.Codec;
import com.github.reload.message.ReloadCodec;
import com.github.reload.message.content.ProbeRequest.ProbeInformationType;
import com.github.reload.message.content.UptimeProbeInformation.UptimeCodec;

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

	public static class UptimeCodec extends Codec<UptimeProbeInformation> {

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
