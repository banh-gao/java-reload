package com.github.reload.message.content;

import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import com.github.reload.Context;
import com.github.reload.message.Content;
import com.github.reload.message.ContentType;
import com.github.reload.message.content.ProbeAnswer.ProbeAnswerCodec;
import com.github.reload.net.data.Codec;
import com.github.reload.net.data.ReloadCodec;

@ReloadCodec(ProbeAnswerCodec.class)
public class ProbeAnswer extends Content {

	private final List<ProbeInformation> probeInfo;

	public ProbeAnswer(List<ProbeInformation> probeInfo) {
		this.probeInfo = probeInfo;
	}

	public List<ProbeInformation> getProbeInformations() {
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

	public static class ProbeAnswerCodec extends Codec<ProbeAnswer> {

		private static final int LIST_LENGTH_FIELD = U_INT16;

		private final Codec<ProbeInformation> infoCodec;

		public ProbeAnswerCodec(Context context) {
			super(context);
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
				probeInfo.add(infoCodec.decode(buf));
			}

			return new ProbeAnswer(probeInfo);
		}

	}

}