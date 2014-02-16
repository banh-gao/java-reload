package com.github.reload.message.content;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import com.github.reload.message.Content;

public class ProbeRequest extends Content {

	private static final int LIST_LENGTH_FIELD = EncUtils.U_INT8;

	public enum ProbeInformationType {
		RESPONSIBLE_SET((byte) 1), NUM_RESOUCES((byte) 2), UPTIME((byte) 3);

		private final byte code;

		private ProbeInformationType(byte code) {
			this.code = code;
		}

		public byte getCode() {
			return code;
		}

		public static ProbeInformationType valueOf(byte v) {
			for (ProbeInformationType t : EnumSet.allOf(ProbeInformationType.class)) {
				if (t.code == v)
					return t;
			}
			return null;
		}
	}

	private final List<ProbeInformationType> requestedInfo;

	public ProbeRequest(ProbeInformationType... probeTypes) {
		this(Arrays.asList(probeTypes));
	}

	public ProbeRequest(List<ProbeInformationType> probeTypes) {
		requestedInfo = probeTypes;
	}

	public ProbeRequest(UnsignedByteBuffer buf) {
		int length = buf.getLengthValue(LIST_LENGTH_FIELD);
		requestedInfo = new ArrayList<ProbeRequest.ProbeInformationType>(length);
		for (int i = 0; i < length; i++) {
			byte typeV = buf.getRaw8();
			ProbeInformationType type = ProbeInformationType.valueOf(typeV);
			if (type != null) {
				requestedInfo.add(type);
			} else
				throw new DecodingException("Unknown probe information type " + typeV);
		}
	}

	public List<ProbeInformationType> getRequestedInfo() {
		return requestedInfo;
	}

	@Override
	protected void implWriteTo(UnsignedByteBuffer buf) {
		Field lenFld = buf.allocateLengthField(LIST_LENGTH_FIELD);
		for (ProbeInformationType t : requestedInfo) {
			buf.putRaw8(t.getCode());
		}
		lenFld.setEncodedLength(buf.getConsumedFrom(lenFld.getNextPosition()));
	}

	@Override
	public ContentType getType() {
		return ContentType.PROBE_REQ;
	}

}
