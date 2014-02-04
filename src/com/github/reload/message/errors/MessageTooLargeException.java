package com.github.reload.message.errors;

import com.github.reload.message.errors.Error.ErrorType;

/**
 * Indicates that the message size exceed the maximum allowed size
 * 
 * @author Daniel Zozin <zdenial@gmx.com>
 * 
 */
public class MessageTooLargeException extends ErrorMessageException {

	public MessageTooLargeException(String message) {
		super(new Error(ErrorType.MESSAGE_TOO_LARGE, message));
	}

}
