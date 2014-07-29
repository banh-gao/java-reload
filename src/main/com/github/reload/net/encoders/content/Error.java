package com.github.reload.net.encoders.content;

import io.netty.buffer.ByteBuf;
import java.nio.charset.Charset;
import java.util.EnumSet;
import com.github.reload.conf.Configuration;
import com.github.reload.net.encoders.Codec;
import com.github.reload.net.encoders.Codec.ReloadCodec;
import com.github.reload.net.encoders.content.Error.ErrorCodec;

/**
 * An RELOAD error message
 * 
 */
@ReloadCodec(ErrorCodec.class)
public class Error extends Content {

	public enum ErrorType {
		FORBITTEN((short) 2),
		NOT_FOUND((short) 3),
		REQUEST_TIMEOUT((short) 4),
		GEN_COUNTER_TOO_LOW((short) 5),
		INCOMPATIBLE_WITH_OVERLAY((short) 6),
		UNSUPPORTED_FWD_OPTION((short) 7),
		DATA_TOO_LARGE((short) 8),
		DATA_TOO_OLD((short) 9),
		TLL_EXCEEDED((short) 10),
		MESSAGE_TOO_LARGE((short) 11),
		UNKNOWN_KIND((short) 12),
		UNKNOWN_EXTENSION((short) 13),
		RESPONSE_TOO_LARGE((short) 14),
		CONFIG_TOO_OLD((short) 15),
		CONFIG_TOO_NEW((short) 16),
		IN_PROGRESS((short) 17),
		INVALID_MESSAGE((short) 20);

		private final short code;

		private ErrorType(short code) {
			this.code = code;
		}

		public static ErrorType valueOf(short code) {
			for (ErrorType t : EnumSet.allOf(ErrorType.class))
				if (t.code == code)
					return t;
			return null;
		}

		public short getCode() {
			return code;
		}
	}

	public static final Charset MSG_CHARSET = Charset.forName("US-ASCII");

	private final ErrorType error;
	private final byte[] info;

	public Error(ErrorType type, String info) {
		error = type;
		this.info = info.getBytes(MSG_CHARSET);
	}

	public Error(ErrorType type, byte[] info) {
		error = type;
		this.info = info;
	}

	public ErrorType getErrorType() {
		return error;
	}

	public String getInfo() {
		return new String(info, MSG_CHARSET);
	}

	public byte[] getEncodedInfo() {
		return info;
	}

	@Override
	public String toString() {
		return "Error " + error.getCode() + " (" + error + "): " + getInfo();
	}

	@Override
	public ContentType getType() {
		return ContentType.ERROR;
	}

	public ErrorMessageException toException() {
		return new ErrorMessageException(this);
	}

	@Override
	public boolean isAnswer() {
		return true;
	}

	static class ErrorCodec extends Codec<Error> {

		private static final int INFO_LENGTH_FIELD = U_INT16;

		public ErrorCodec(Configuration conf) {
			super(conf);
		}

		@Override
		public void encode(Error obj, ByteBuf buf, Object... params) throws CodecException {
			buf.writeShort(obj.error.getCode());

			Field lenFld = allocateField(buf, INFO_LENGTH_FIELD);
			buf.writeBytes(obj.info);
			lenFld.updateDataLength();
		}

		@Override
		public Error decode(ByteBuf buf, Object... params) throws CodecException {
			ErrorType error = ErrorType.valueOf(buf.readShort());

			ByteBuf info = readField(buf, INFO_LENGTH_FIELD);
			byte[] infoData = new byte[info.readableBytes()];
			info.readBytes(infoData);

			return new Error(error, infoData);

		}

	}

	public static class ErrorMessageException extends Exception {

		private final ErrorType type;
		private final byte[] info;

		public ErrorMessageException(Error error) {
			super(error.getInfo());
			this.type = error.getErrorType();
			this.info = error.getEncodedInfo();
		}

		public ErrorType getType() {
			return type;
		}

		public byte[] getInfo() {
			return info;
		}
	}
}
