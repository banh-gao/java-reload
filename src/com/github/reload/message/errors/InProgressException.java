package com.github.reload.message.errors;


/**
 * Indicates an already in progress operation
 * 
 */
public class InProgressException extends Exception implements ErrorRespose {

	public InProgressException(String message) {
		super(message);
	}

	@Override
	public ErrorType getErrorType() {
		return ErrorType.IN_PROGRESS;
	}
}
