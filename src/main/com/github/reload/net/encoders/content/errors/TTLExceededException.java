package com.github.reload.net.encoders.content.errors;

/**
 * Indicates that the time to live of the message reaches zero and the message
 * is dropped
 * 
 * @author Daniel Zozin <zdenial@gmx.com>
 * 
 */
public class TTLExceededException extends Exception implements ErrorRespose {

	public TTLExceededException(String message) {
		super(message);
	}

	@Override
	public ErrorType getErrorType() {
		return ErrorType.TLL_EXCEEDED;
	}
}
