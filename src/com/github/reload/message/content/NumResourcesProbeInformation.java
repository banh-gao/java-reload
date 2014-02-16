package com.github.reload.message.content;

import com.github.reload.message.content.ProbeAnswer.ProbeInformation;
import com.github.reload.message.content.ProbeRequest.ProbeInformationType;

/**
 * Probe information that reports the amount of resources stored locally
 * 
 * @author Daniel Zozin <zdenial@gmx.com>
 * 
 */
public class NumResourcesProbeInformation extends ProbeInformation {

	private final long numResources;

	public NumResourcesProbeInformation(int resourceNumber) {
		numResources = resourceNumber;
	}

	public NumResourcesProbeInformation(UnsignedByteBuffer buf) {
		numResources = buf.getSigned32();
	}

	@Override
	public ProbeInformationType getType() {
		return ProbeInformationType.NUM_RESOUCES;
	}

	@Override
	protected void implWriteTo(UnsignedByteBuffer buf) {
		buf.putUnsigned32(numResources);
	}

	public long getResourceNumber() {
		return numResources;
	}

	@Override
	public String toString() {
		return "NumResourcesProbeInformation [numResources=" + numResources + "]";
	}
}
