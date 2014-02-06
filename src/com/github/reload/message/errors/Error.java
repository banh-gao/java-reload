package com.github.reload.message.errors;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import java.io.UnsupportedEncodingException;
import java.util.EnumSet;
import com.github.reload.message.Content;
import com.github.reload.message.ForwardingOptionCodec.UnsupportedFwdOptionException;
import com.github.reload.message.content.ContentType;
import com.github.reload.net.data.CodecUtils;

/**
 * An RELOAD error message
 * 
 * @author Daniel Zozin <zdenial@gmx.com>
 * 
 */
public class Error extends Content {

	private static final int INFO_LENGTH_FIELD = CodecUtils.U_INT16;

	public enum ErrorType {
		FORBITTEN((short) 2) {

			@Override
			public ErrorMessageException toException(Error error) {
				return new ForbittenException(error.getStringInfo());
			}
		},
		NOT_FOUND((short) 3) {

			@Override
			public ErrorMessageException toException(Error error) {
				return new NotFoundException(error.getStringInfo());
			}
		},
		REQUEST_TIMEOUT((short) 4) {

			@Override
			public ErrorMessageException toException(Error error) {
				return new RequestTimeoutException(error.getStringInfo());
			}
		},
		GEN_COUNTER_TOO_LOW((short) 5) {

			@Override
			public ErrorMessageException toException(Error error) {
				try {
					return new GenerationTooLowException(error.getInfo());
				} catch (DecoderException e) {
					return new StorageException(error.getErrorType() + " #INVALID ERROR CONTENT#");
				}
			}
		},
		INCOMPATIBLE_WITH_OVERLAY((short) 6) {

			@Override
			public ErrorMessageException toException(Error error) {
				return new IncompatibleOverlayException(error.getStringInfo());
			}
		},
		UNSUPPORTED_FWD_OPTION((short) 7) {

			@Override
			public ErrorMessageException toException(Error error) {
				return new UnsupportedFwdOptionException(error.getStringInfo());
			}
		},
		DATA_TOO_LARGE((short) 8) {

			@Override
			public ErrorMessageException toException(Error error) {
				return new DataTooLargeException(error.getStringInfo());
			}
		},
		DATA_TOO_OLD((short) 9) {

			@Override
			public ErrorMessageException toException(Error error) {
				return new DataTooOldException(error.getStringInfo());
			}
		},
		TLL_EXCEEDED((short) 10) {

			@Override
			public ErrorMessageException toException(Error error) {
				return new TTLExceededException(error.getStringInfo());
			}
		},
		MESSAGE_TOO_LARGE((short) 11) {

			@Override
			public ErrorMessageException toException(Error error) {
				return new MessageTooLargeException(error.getStringInfo());
			}
		},
		UNKNOWN_KIND((short) 12) {

			@Override
			public ErrorMessageException toException(Error error) {
				try {
					return new UnknownKindException(error.getInfo());
				} catch (DecoderException e) {
					return new StorageException(error.getErrorType() + " #INVALID ERROR CONTENT#");
				}
			}
		},
		UNKNOWN_EXTENSION((short) 13) {

			@Override
			public ErrorMessageException toException(Error error) {
				return new UnknownExtensionException(error.getStringInfo());
			}
		},
		RESPONSE_TOO_LARGE((short) 14) {

			@Override
			public ErrorMessageException toException(Error error) {
				return new ResponseTooLargeException(error.getStringInfo());
			}
		},
		CONFIG_TOO_OLD((short) 15) {

			@Override
			public ErrorMessageException toException(Error error) {
				return new ConfigurationTooOldException(error.getStringInfo());
			}
		},
		CONFIG_TOO_NEW((short) 16) {

			@Override
			public ErrorMessageException toException(Error error) {
				return new ConfigurationTooNewException(error.getStringInfo());
			}
		},
		IN_PROGRESS((short) 17) {

			@Override
			public ErrorMessageException toException(Error error) {
				return new InProgressException(error.getStringInfo());
			}
		},
		INVALID_MESSAGE((short) 20) {

			@Override
			public ErrorMessageException toException(Error error) {
				return new InvalidMessageException(error.getStringInfo());
			}
		},
		UNDERLAY_DEST_UNREACHABLE((short) 101) {

			@Override
			public ErrorMessageException toException(Error error) {
				return new DiagnosticErrorException(error);
			}
		},
		UNDERLAY_TIME_EXCEEDED((short) 102) {

			@Override
			public ErrorMessageException toException(Error error) {
				return new DiagnosticErrorException(error);
			}
		},
		MESSAGE_EXPIRED((short) 103) {

			@Override
			public ErrorMessageException toException(Error error) {
				return new DiagnosticErrorException(error);
			}
		},
		UPSTREAM_MISROUTING((short) 104) {

			@Override
			public ErrorMessageException toException(Error error) {
				return new DiagnosticErrorException(error);
			}
		},
		LOOP_DETECTED((short) 105) {

			@Override
			public ErrorMessageException toException(Error error) {
				return new DiagnosticErrorException(error);
			}
		},
		TTL_HOPS_EXCEEDED((short) 106) {

			@Override
			public ErrorMessageException toException(Error error) {
				return new DiagnosticErrorException(error);
			}
		};

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

		public abstract ErrorMessageException toException(Error error);
	}

	private final ErrorType error;
	private final byte[] info;

	public Error(ErrorType type, String info) {
		error = type;
		try {
			this.info = info.getBytes("US-ASCII");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	public Error(ErrorType type, byte[] info) {
		error = type;
		this.info = info;
	}

	public Error(ByteBuf buf) {
		error = ErrorType.valueOf(buf.getRaw16());

		int len = buf.getLengthValue(Error.INFO_LENGTH_FIELD);
		info = new byte[len];
		buf.getRaw(info);
	}

	public ErrorType getErrorType() {
		return error;
	}

	public byte[] getInfo() {
		return info;
	}

	public String getStringInfo() {
		return new String(info);
	}

	@Override
	public String toString() {
		return "Error " + error.getCode() + " (" + error + "): " + getStringInfo();
	}

	@Override
	protected void implEncode(ByteBuf buf) {
		buf.putRaw16(error.getCode());
		Field lenFld = buf.allocateLengthField(Error.INFO_LENGTH_FIELD);
		buf.putRaw(info);
		lenFld.setEncodedLength(buf.getConsumedFrom(lenFld.getNextPosition()));
	}

	@Override
	public ContentType getType() {
		return ContentType.ERROR;
	}

	public ErrorMessageException toException() {
		return error.toException(this);
	}

	@Override
	public boolean isAnswer() {
		return true;
	}
}
