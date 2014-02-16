package com.github.reload.message.errors;


/**
 * Indicates a problem with some overlay parameter
 * 
 */
public class IncompatibleOverlayException extends Exception implements ErrorRespose {

	public IncompatibleOverlayException(String message) {
		super(message);
	}

	@Override
	public ErrorType getErrorType() {
		return ErrorType.INCOMPATIBLE_WITH_OVERLAY;
	}
}
