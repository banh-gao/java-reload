package com.github.reload.net.codecs.content;

import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import dagger.ObjectGraph;
import com.github.reload.net.codecs.Codec;
import com.github.reload.net.codecs.Codec.ReloadCodec;
import com.github.reload.net.codecs.content.ProbeAnswer.ProbeAnswerCodec;

@ReloadCodec(ProbeAnswerCodec.class)
public class ProbeAnswer extends Content {

	private final List<? extends ProbeInformation> probeInfo;

	public ProbeAnswer(List<? extends ProbeInformation> probeInfo) {
		this.probeInfo = probeInfo;
	}

	public List<? extends ProbeInformation> getProbeInformations() {
		return probeInfo;
	}

	@Override
	public ContentType getType() {
		return ContentType.PROBE_ANS;
	}

	@Override
	public String toString() {
		return "ProbeAnswer [info=" + probeInfo + "]";
	}

	static class ProbeAnswerCodec extends Codec<ProbeAnswer> {

		private static final int LIST_LENGTH_FIELD = U_INT16;

		private final Codec<ProbeInformation> infoCodec;

		public ProbeAnswerCodec(ObjectGraph ctx) {
			super(ctx);
			infoCodec = getCodec(ProbeInformation.class);
		}

		@Override
		public void encode(ProbeAnswer obj, ByteBuf buf, Object... params) throws CodecException {
			Field lenFld = allocateField(buf, LIST_LENGTH_FIELD);

			for (ProbeInformation info : obj.probeInfo) {
				infoCodec.encode(info, buf);
			}

			lenFld.updateDataLength();
		}

		@Override
		public ProbeAnswer decode(ByteBuf buf, Object... params) throws CodecException {
			List<ProbeInformation> probeInfo = new ArrayList<ProbeInformation>();

			ByteBuf probeInfoData = readField(buf, LIST_LENGTH_FIELD);

			while (probeInfoData.readableBytes() > 0) {
				ProbeInformation info = infoCodec.decode(probeInfoData);
				probeInfo.add(info);
			}

			return new ProbeAnswer(probeInfo);
		}

	}

}