package com.github.reload.message.content;

import java.util.EnumSet;
import net.sf.jReload.message.ContentType;
import net.sf.jReload.message.DecodingException;
import net.sf.jReload.message.EncUtils;
import net.sf.jReload.message.MessageContent;
import net.sf.jReload.message.UnsignedByteBuffer;
import net.sf.jReload.message.UnsignedByteBuffer.Field;

public class ConfigUpdateRequest extends MessageContent {

	private static final int CONF_LENGTH_FIELD = EncUtils.U_INT24;

	public enum ConfigUpdateType {
		CONFIG((byte) 1), KIND((byte) 2);

		private final byte code;

		private ConfigUpdateType(byte code) {
			this.code = code;
		}

		public static ConfigUpdateType valueOf(byte code) {
			for (ConfigUpdateType t : EnumSet.allOf(ConfigUpdateType.class))
				if (t.code == code)
					return t;
			return null;
		}
	}

	private final ConfigUpdateType type;
	private final byte[] xmlConfigurationData;

	public ConfigUpdateRequest(ConfigUpdateType type, byte[] xmlConfigurationData) {
		this.type = type;
		this.xmlConfigurationData = xmlConfigurationData;
	}

	public ConfigUpdateRequest(UnsignedByteBuffer buf) {
		type = ConfigUpdateType.valueOf(buf.getRaw8());

		@SuppressWarnings("unused")
		long length = buf.getSigned32();

		if (type == null)
			throw new DecodingException("Unknown configuration type");

		xmlConfigurationData = readConfigData(buf);
	}

	private static byte[] readConfigData(UnsignedByteBuffer buf) {
		int confLength = buf.getLengthValue(CONF_LENGTH_FIELD);
		byte[] xmlConfigurationData = new byte[confLength];
		buf.getRaw(xmlConfigurationData);
		return xmlConfigurationData;
	}

	@Override
	protected void implWriteTo(UnsignedByteBuffer buf) {
		buf.putRaw8(type.code);

		buf.putUnsigned32(CONF_LENGTH_FIELD + xmlConfigurationData.length);
		Field lenFld = buf.allocateLengthField(CONF_LENGTH_FIELD);
		buf.putRaw(xmlConfigurationData);
		lenFld.setEncodedLength(buf.getConsumedFrom(lenFld.getNextPosition()));
	}

	@Override
	public ContentType getType() {
		return ContentType.CONFIG_UPDATE_REQ;
	}

	public ConfigUpdateType getConfigurationType() {
		return type;
	}

	public byte[] getXmlConfigurationData() {
		return xmlConfigurationData;
	}
}
