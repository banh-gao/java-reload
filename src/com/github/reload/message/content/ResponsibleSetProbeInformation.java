package com.github.reload.message.content;

import com.github.reload.message.content.ProbeAnswer.ProbeInformation;
import com.github.reload.message.content.ProbeRequest.ProbeInformationType;

/**
 * Probe information that reports the magnitude of the resource space this peer
 * is responsible for in part per billion over the total resource keyspace
 * 
 * @author Daniel Zozin <zdenial@gmx.com>
 * 
 */
public class ResponsibleSetProbeInformation extends ProbeInformation {

	private final long responsiblePpb;

	public ResponsibleSetProbeInformation(long responsiblePartPerBillion) {
		responsiblePpb = responsiblePartPerBillion;
	}

	public ResponsibleSetProbeInformation(UnsignedByteBuffer buf) {
		responsiblePpb = buf.getSigned32();
	}

	@Override
	public ProbeInformationType getType() {
		return ProbeInformationType.RESPONSIBLE_SET;
	}

	public long getResponsiblePartPerBillion() {
		return responsiblePpb;
	}

	@Override
	protected void implWriteTo(UnsignedByteBuffer buf) {
		buf.putUnsigned32(responsiblePpb);
	}

	@Override
	public String toString() {
		return "ResponsibleSetProbeInformation [responsiblePpb=" + responsiblePpb + "]";
	}

}
