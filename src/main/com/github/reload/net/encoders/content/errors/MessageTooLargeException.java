package com.github.reload.net.encoders.content.errors;

/**
 * Indicates that the message size exceed the maximum allowed size
 * 
 * @author Daniel Zozin <zdenial@gmx.com>
 * 
 */
public class MessageTooLargeException extends Exception implements ErrorRespose {

	public MessageTooLargeException(String message) {
		super(message);
	}

	@Override
	public ErrorType getErrorType() {
		return ErrorType.MESSAGE_TOO_LARGE;
	}

}
