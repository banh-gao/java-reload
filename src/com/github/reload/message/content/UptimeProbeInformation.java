package com.github.reload.message.content;

import io.netty.buffer.ByteBuf;
import com.github.reload.Context;
import com.github.reload.message.content.ProbeRequest.ProbeInformationType;
import com.github.reload.message.content.UptimeProbeInformation.UptimeCodec;
import com.github.reload.net.data.Codec;
import com.github.reload.net.data.ReloadCodec;

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

		public UptimeCodec(Context context) {
			super(context);
		}

		@Override
		public void encode(UptimeProbeInformation obj, ByteBuf buf) throws CodecException {
			buf.writeInt((int) obj.uptime);
		}

		@Override
		public UptimeProbeInformation decode(ByteBuf buf) throws CodecException {
			return new UptimeProbeInformation(buf.readUnsignedInt());
		}

	}

}
