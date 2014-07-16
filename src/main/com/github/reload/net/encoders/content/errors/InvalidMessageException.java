package com.github.reload.net.encoders.content.errors;

import com.github.reload.net.encoders.Codec.CodecException;

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
