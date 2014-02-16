package com.github.reload.message.errors;

import com.github.reload.net.data.Codec.CodecException;

/**
 * Indicates an invalid RELOAD message
 */
public class InvalidMessageException extends CodecException {

	public InvalidMessageException(String message) {
		super(message);
	}

	@Override
	public ErrorType getErrorType() {
		return ErrorType.INVALID_MESSAGE;
	}

}
