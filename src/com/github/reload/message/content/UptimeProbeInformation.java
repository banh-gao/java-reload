package com.github.reload.message.content;

import net.sf.jReload.message.UnsignedByteBuffer;
import net.sf.jReload.message.content.ProbeAnswer.ProbeInformation;
import net.sf.jReload.message.content.ProbeRequest.ProbeInformationType;

/**
 * Probe information that reports the number of seconds this peer is online
 * 
 * @author Daniel Zozin <zdenial@gmx.com>
 * 
 */
public class UptimeProbeInformation extends ProbeInformation {

	private final long uptime;

	public UptimeProbeInformation(int uptime) {
		this.uptime = uptime;
	}

	public UptimeProbeInformation(UnsignedByteBuffer buf) {
		uptime = buf.getSigned32();
	}

	@Override
	public ProbeInformationType getType() {
		return ProbeInformationType.UPTIME;
	}

	@Override
	protected void implWriteTo(UnsignedByteBuffer buf) {
		buf.putUnsigned32(uptime);
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

}
