package com.github.reload.message.errors;


/**
 * Indicates a message that exceeds the maximum requested response size
 * 
 */
public class ResponseTooLargeException extends Exception implements ErrorRespose {

	public ResponseTooLargeException(String message) {
		super(message);
	}

	@Override
	public ErrorType getErrorType() {
		return ErrorType.RESPONSE_TOO_LARGE;
	}

}
