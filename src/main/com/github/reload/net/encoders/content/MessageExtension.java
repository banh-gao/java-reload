package com.github.reload.net.encoders.content;

import io.netty.buffer.ByteBuf;
import java.util.EnumSet;
import com.github.reload.Configuration;
import com.github.reload.net.encoders.Codec;
import com.github.reload.net.encoders.Codec.CodecException;
import com.github.reload.net.encoders.Codec.ReloadCodec;
import com.github.reload.net.encoders.content.MessageExtension.MessageExtensionCodec;
import com.github.reload.net.encoders.content.errors.ErrorType;

/**
 * Message extension contained in the message content
 */
@ReloadCodec(MessageExtensionCodec.class)
public abstract class MessageExtension {

	public enum MessageExtensionType {
		UNKNOWN((short) 0);

		private final short code;

		private MessageExtensionType(short code) {
			this.code = code;
		}

		public static MessageExtensionType valueOf(short code) {
			for (MessageExtensionType t : EnumSet.allOf(MessageExtensionType.class)) {
				if (t.code == code)
					return t;
			}
			return UNKNOWN;
		}
	}

	protected abstract MessageExtensionType getExtensionType();

	protected boolean isCritical;

	public boolean isCritical() {
		return isCritical;
	}

	/**
	 * Indicates an unknown message extension critical for message processing
	 * 
	 */
	public static class UnknownExtensionException extends CodecException {

		public UnknownExtensionException(String message) {
			super(message);
		}

		@Override
		public ErrorType getErrorType() {
			return ErrorType.UNKNOWN_EXTENSION;
		}
	}

	public static class MessageExtensionCodec extends Codec<MessageExtension> {

		private static final int EXTENSION_CONTENT_LENGTH_FIELD = U_INT32;

		public MessageExtensionCodec(Configuration conf) {
			super(conf);
		}

		@Override
		public void encode(MessageExtension obj, ByteBuf buf, Object... params) throws CodecException {
			buf.writeShort(obj.getExtensionType().code);
			buf.writeByte(obj.isCritical ? 1 : 0);

			Field lenFld = allocateField(buf, EXTENSION_CONTENT_LENGTH_FIELD);

			@SuppressWarnings("unchecked")
			Codec<MessageExtension> codec = (Codec<MessageExtension>) getCodec(obj.getClass());

			codec.encode(obj, buf);

			lenFld.updateDataLength();
		}

		@Override
		public MessageExtension decode(ByteBuf buf, Object... params) throws CodecException {
			MessageExtensionType type = MessageExtensionType.valueOf(buf.readShort());

			boolean isCritical = (buf.readUnsignedByte() > 0);

			MessageExtension extension = null;

			buf = readField(buf, EXTENSION_CONTENT_LENGTH_FIELD);

			if (type == MessageExtensionType.UNKNOWN && isCritical)
				throw new UnknownExtensionException("Unsupported message extension");

			switch (type) {
				case UNKNOWN :
					extension = buildUnknownExtension();
					break;
			}

			assert (extension != null);

			extension.isCritical = isCritical;
			return extension;
		}

		private static MessageExtension buildUnknownExtension() {
			return new MessageExtension() {

				@Override
				protected MessageExtensionType getExtensionType() {
					return MessageExtensionType.UNKNOWN;
				}
			};
		}

	}

}
