package com.github.reload.message.errors;

import com.github.reload.message.errors.Error.ErrorType;

/**
 * Indicates that the time to live of the message reaches zero and the message
 * is dropped
 * 
 * @author Daniel Zozin <zdenial@gmx.com>
 * 
 */
public class TTLExceededException extends ErrorMessageException {

	public TTLExceededException(String message) {
		super(new Error(ErrorType.TLL_EXCEEDED, message));
	}
}
