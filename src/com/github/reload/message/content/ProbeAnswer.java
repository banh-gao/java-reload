package com.github.reload.message.content;

import java.util.ArrayList;
import java.util.List;
import net.sf.jReload.message.ContentType;
import net.sf.jReload.message.DecodingException;
import net.sf.jReload.message.EncUtils;
import net.sf.jReload.message.MessageContent;
import net.sf.jReload.message.UnsignedByteBuffer;
import net.sf.jReload.message.UnsignedByteBuffer.Field;
import net.sf.jReload.message.content.ProbeRequest.ProbeInformationType;

public class ProbeAnswer extends MessageContent {

	private static final int LIST_LENGTH_FIELD = EncUtils.U_INT16;

	private final List<ProbeInformation> probeInfo;

	public ProbeAnswer(List<ProbeInformation> probeInfo) {
		this.probeInfo = probeInfo;
	}

	public ProbeAnswer(UnsignedByteBuffer buf) {
		probeInfo = new ArrayList<ProbeAnswer.ProbeInformation>();

		int length = buf.getLengthValue(LIST_LENGTH_FIELD);

		int startPos = buf.position();

		while (buf.getConsumedFrom(startPos) < length) {
			ProbeInformation info = ProbeInformation.parse(buf);
			probeInfo.add(info);
		}
	}

	@Override
	protected void implWriteTo(UnsignedByteBuffer buf) {
		Field lenFld = buf.allocateLengthField(LIST_LENGTH_FIELD);

		for (ProbeInformation info : probeInfo) {
			info.writeTo(buf);
		}

		lenFld.setEncodedLength(buf.getConsumedFrom(lenFld.getNextPosition()));
	}

	public List<ProbeInformation> getProbeInformations() {
		return probeInfo;
	}

	@Override
	public ContentType getType() {
		return ContentType.PROBE_ANS;
	}

	public static abstract class ProbeInformation {

		private static final int INFORMATION_LENGTH_FIELD = EncUtils.U_INT8;

		protected abstract ProbeInformationType getType();

		protected abstract void implWriteTo(UnsignedByteBuffer buf);

		public static ProbeInformation parse(UnsignedByteBuffer buf) {
			byte typeV = buf.getRaw8();
			ProbeInformationType type = ProbeInformationType.valueOf(typeV);

			if (type == null)
				throw new DecodingException("Unknown probe information type " + typeV);

			@SuppressWarnings("unused")
			short length = buf.getSigned8();

			switch (type) {
				case RESPONSIBLE_SET :
					return new ResponsibleSetProbeInformation(buf);
				case NUM_RESOUCES :
					return new NumResourcesProbeInformation(buf);
				case UPTIME :
					return new UptimeProbeInformation(buf);
			}

			throw new RuntimeException("Unknown probe information type " + type);
		}

		public void writeTo(UnsignedByteBuffer buf) {
			buf.putRaw8(getType().getCode());
			Field lenFld = buf.allocateLengthField(INFORMATION_LENGTH_FIELD);

			implWriteTo(buf);

			lenFld.setEncodedLength(buf.getConsumedFrom(lenFld.getNextPosition()));
		}
	}

	@Override
	public String toString() {
		return "ProbeAnswer [info=" + probeInfo + "]";
	}

}