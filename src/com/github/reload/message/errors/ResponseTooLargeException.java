package com.github.reload.message.errors;

import com.github.reload.message.errors.Error.ErrorType;

/**
 * Indicates a message that exceeds the maximum requested response size
 * 
 * @author Daniel Zozin <zdenial@gmx.com>
 * 
 */
public class ResponseTooLargeException extends ErrorMessageException {

	public ResponseTooLargeException(String message) {
		super(new Error(ErrorType.RESPONSE_TOO_LARGE, message));
	}

}
