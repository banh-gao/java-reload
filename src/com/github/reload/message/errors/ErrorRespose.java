package com.github.reload.message.errors;


/**
 * An error response to send to the sender node with the cause of the error
 */
public interface ErrorRespose {

	public ErrorType getErrorType();
}
