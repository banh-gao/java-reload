package com.github.reload.message.errors;

import com.github.reload.message.errors.Error.ErrorType;

/**
 * Indicates an invalid RELOAD message
 * 
 * @author Daniel Zozin <zdenial@gmx.com>
 * 
 */
public class InvalidMessageException extends ErrorMessageException {

	public InvalidMessageException(String message) {
		super(new Error(ErrorType.INVALID_MESSAGE, message));
	}
}
