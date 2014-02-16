package com.github.reload.message.errors;

import com.github.reload.message.errors.Error.ErrorType;
import com.github.reload.net.data.Codec.CodecException;

/**
 * Indicates an invalid RELOAD message
 */
public class InvalidMessageException extends CodecException {

	public InvalidMessageException(String message) {
		super();
	}

	@Override
	public Error getError() {
		return new Error(ErrorType.INVALID_MESSAGE, getMessage());
	}

}
