package com.github.reload.message.errors;


/**
 * Indicates that the request times out
 * 
 */
public class RequestTimeoutException extends Exception implements ErrorRespose {

	public RequestTimeoutException(String message) {
		super(message);
	}

	@Override
	public ErrorType getErrorType() {
		return ErrorType.REQUEST_TIMEOUT;
	}
}
