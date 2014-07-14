package com.github.reload.message.content;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import com.github.reload.Configuration;
import com.github.reload.message.Codec;
import com.github.reload.message.Content;
import com.github.reload.message.ContentType;
import com.github.reload.message.Codec.ReloadCodec;
import com.github.reload.message.content.ProbeRequest.ProbeRequestCodec;

@ReloadCodec(ProbeRequestCodec.class)
public class ProbeRequest extends Content {

	private final List<ProbeInformationType> requestedInfo;

	public ProbeRequest(List<ProbeInformationType> probeTypes) {
		requestedInfo = probeTypes;
	}

	public ProbeRequest(ProbeInformationType... probeTypes) {
		this(Arrays.asList(probeTypes));
	}

	public List<ProbeInformationType> getRequestedInfo() {
		return requestedInfo;
	}

	@Override
	public ContentType getType() {
		return ContentType.PROBE_REQ;
	}

	public enum ProbeInformationType {
		RESPONSIBLE_SET((byte) 0x1, ResponsibleSetProbeInformation.class),
		NUM_RESOUCES((byte) 0x2, NumResourcesProbeInformation.class),
		UPTIME((byte) 0x3, UptimeProbeInformation.class);

		public static ProbeInformationType valueOf(byte v) {
			for (ProbeInformationType t : EnumSet.allOf(ProbeInformationType.class)) {
				if (t.code == v)
					return t;
			}
			return null;
		}

		private final byte code;

		private final Class<? extends ProbeInformation> infoClass;

		private ProbeInformationType(byte code, Class<? extends ProbeInformation> infoClass) {
			this.code = code;
			this.infoClass = infoClass;
		}

		public byte getCode() {
			return code;
		}

		public Class<? extends ProbeInformation> getInfoClass() {
			return infoClass;
		}
	}

	public static class ProbeRequestCodec extends Codec<ProbeRequest> {

		private static final int LIST_LENGTH_FIELD = U_INT8;

		public ProbeRequestCodec(Configuration conf) {
			super(conf);
		}

		@Override
		public ProbeRequest decode(ByteBuf buf, Object... params) throws CodecException {

			List<ProbeInformationType> requestedInfo = new ArrayList<ProbeRequest.ProbeInformationType>();

			ByteBuf reqInfoData = readField(buf, LIST_LENGTH_FIELD);

			while (reqInfoData.readableBytes() > 0) {
				byte typeV = reqInfoData.readByte();
				ProbeInformationType type = ProbeInformationType.valueOf(typeV);

				if (type == null)
					throw new DecoderException("Unknown probe information type " + typeV);

				requestedInfo.add(type);
			}

			reqInfoData.release();

			return new ProbeRequest(requestedInfo);
		}

		@Override
		public void encode(ProbeRequest obj, ByteBuf buf, Object... params) throws CodecException {
			Field lenFld = allocateField(buf, LIST_LENGTH_FIELD);
			for (ProbeInformationType t : obj.requestedInfo) {
				buf.writeByte(t.getCode());
			}
			lenFld.updateDataLength();
		}

	}
}
