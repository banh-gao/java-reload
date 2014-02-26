package com.github.reload.message.content;

import io.netty.buffer.ByteBuf;
import com.github.reload.Configuration;
import com.github.reload.message.content.NumResourcesProbeInformation.NumResCodec;
import com.github.reload.message.content.ProbeRequest.ProbeInformationType;
import com.github.reload.net.data.Codec;
import com.github.reload.net.data.ReloadCodec;

/**
 * Probe information that reports the amount of resources stored locally
 * 
 */
@ReloadCodec(NumResCodec.class)
public class NumResourcesProbeInformation extends ProbeInformation {

	private final long numResources;

	public NumResourcesProbeInformation(long resourceNumber) {
		numResources = resourceNumber;
	}

	@Override
	public ProbeInformationType getType() {
		return ProbeInformationType.NUM_RESOUCES;
	}

	public long getResourceNumber() {
		return numResources;
	}

	@Override
	public String toString() {
		return "NumResourcesProbeInformation [numResources=" + numResources + "]";
	}

	public static class NumResCodec extends Codec<NumResourcesProbeInformation> {

		public NumResCodec(Configuration conf) {
			super(conf);
		}

		@Override
		public void encode(NumResourcesProbeInformation obj, ByteBuf buf, Object... params) throws CodecException {
			buf.writeInt((int) obj.numResources);
		}

		@Override
		public NumResourcesProbeInformation decode(ByteBuf buf, Object... params) throws CodecException {
			return new NumResourcesProbeInformation(buf.readUnsignedInt());
		}
	}
}
