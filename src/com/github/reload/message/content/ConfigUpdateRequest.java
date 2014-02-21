package com.github.reload.message.content;

import io.netty.buffer.ByteBuf;
import java.util.EnumSet;
import com.github.reload.Context;
import com.github.reload.message.Content;
import com.github.reload.message.ContentType;
import com.github.reload.message.content.ConfigUpdateRequest.ConfUpdateReqCodec;
import com.github.reload.net.data.Codec;
import com.github.reload.net.data.ReloadCodec;

@ReloadCodec(ConfUpdateReqCodec.class)
public class ConfigUpdateRequest extends Content {

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

	public static class ConfUpdateReqCodec extends Codec<ConfigUpdateRequest> {

		private static final int CONF_LENGTH_FIELD = U_INT24;

		public ConfUpdateReqCodec(Context context) {
			super(context);
		}

		@Override
		public void encode(ConfigUpdateRequest obj, ByteBuf buf, Object... params) throws com.github.reload.net.data.Codec.CodecException {
			buf.writeByte(obj.type.code);

			buf.writeInt(CONF_LENGTH_FIELD + obj.xmlConfigurationData.length);
			Field lenFld = allocateField(buf, CONF_LENGTH_FIELD);
			buf.writeBytes(obj.xmlConfigurationData);
			lenFld.updateDataLength();
		}

		@Override
		public ConfigUpdateRequest decode(ByteBuf buf, Object... params) throws com.github.reload.net.data.Codec.CodecException {

			ConfigUpdateType type = ConfigUpdateType.valueOf(buf.readByte());

			if (type == null)
				throw new CodecException("Unknown configuration type");

			// Not used length field (only for backward compatibility)
			buf.readUnsignedInt();

			byte[] xmlConfigurationData = readConfigData(buf);
			return new ConfigUpdateRequest(type, xmlConfigurationData);
		}

		private static byte[] readConfigData(ByteBuf buf) {
			ByteBuf confData = readField(buf, CONF_LENGTH_FIELD);
			byte[] xmlConfigurationData = new byte[confData.readableBytes()];
			buf.readBytes(xmlConfigurationData);
			confData.release();
			return xmlConfigurationData;
		}

	}
}
