package com.github.reload.net.codecs.content;

import io.netty.buffer.ByteBuf;
import dagger.ObjectGraph;
import com.github.reload.net.codecs.Codec;
import com.github.reload.net.codecs.Codec.ReloadCodec;
import com.github.reload.net.codecs.content.NumResourcesProbeInformation.NumResCodec;
import com.github.reload.net.codecs.content.ProbeRequest.ProbeInformationType;

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

	static class NumResCodec extends Codec<NumResourcesProbeInformation> {

		public NumResCodec(ObjectGraph ctx) {
			super(ctx);
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
