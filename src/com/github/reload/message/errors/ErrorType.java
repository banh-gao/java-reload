package com.github.reload.message.errors;

import java.util.EnumSet;
import com.github.reload.message.ForwardingOptionCodec.UnsupportedFwdOptionException;
import com.github.reload.message.MessageExtension.UnknownExtensionException;
import com.github.reload.storage.ForbittenException;
import com.github.reload.storage.errors.DataTooLargeException;
import com.github.reload.storage.errors.DataTooOldException;
import com.github.reload.storage.errors.GenerationTooLowException;
import com.github.reload.storage.errors.NotFoundException;
import com.github.reload.storage.errors.UnknownKindException;

public enum ErrorType {
	FORBITTEN((short) 2) {

		@Override
		public Exception toException(Error error) {
			return new ForbittenException(error.getInfo());
		}
	},
	NOT_FOUND((short) 3) {

		@Override
		public Exception toException(Error error) {
			return new NotFoundException(error.getInfo());
		}
	},
	REQUEST_TIMEOUT((short) 4) {

		@Override
		public Exception toException(Error error) {
			return new RequestTimeoutException(error.getInfo());
		}
	},
	GEN_COUNTER_TOO_LOW((short) 5) {

		@Override
		public Exception toException(Error error) {
			return new GenerationTooLowException(error.getInfo());
		}
	},
	INCOMPATIBLE_WITH_OVERLAY((short) 6) {

		@Override
		public Exception toException(Error error) {
			return new IncompatibleOverlayException(error.getInfo());
		}
	},
	UNSUPPORTED_FWD_OPTION((short) 7) {

		@Override
		public Exception toException(Error error) {
			return new UnsupportedFwdOptionException(error.getInfo());
		}
	},
	DATA_TOO_LARGE((short) 8) {

		@Override
		public Exception toException(Error error) {
			return new DataTooLargeException(error.getInfo());
		}
	},
	DATA_TOO_OLD((short) 9) {

		@Override
		public Exception toException(Error error) {
			return new DataTooOldException(error.getInfo());
		}
	},
	TLL_EXCEEDED((short) 10) {

		@Override
		public Exception toException(Error error) {
			return new TTLExceededException(error.getInfo());
		}
	},
	MESSAGE_TOO_LARGE((short) 11) {

		@Override
		public Exception toException(Error error) {
			return new MessageTooLargeException(error.getInfo());
		}
	},
	UNKNOWN_KIND((short) 12) {

		@Override
		public Exception toException(Error error) {
			return new UnknownKindException(error.getInfo());
		}
	},
	UNKNOWN_EXTENSION((short) 13) {

		@Override
		public Exception toException(Error error) {
			return new UnknownExtensionException(error.getInfo());
		}
	},
	RESPONSE_TOO_LARGE((short) 14) {

		@Override
		public Exception toException(Error error) {
			return new ResponseTooLargeException(error.getInfo());
		}
	},
	CONFIG_TOO_OLD((short) 15) {

		@Override
		public Exception toException(Error error) {
			return new ConfigurationTooOldException(error.getInfo());
		}
	},
	CONFIG_TOO_NEW((short) 16) {

		@Override
		public Exception toException(Error error) {
			return new ConfigurationTooNewException(error.getInfo());
		}
	},
	IN_PROGRESS((short) 17) {

		@Override
		public Exception toException(Error error) {
			return new InProgressException(error.getInfo());
		}
	},
	INVALID_MESSAGE((short) 20) {

		@Override
		public Exception toException(Error error) {
			return new InvalidMessageException(error.getInfo());
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

	public abstract Exception toException(Error error);
}