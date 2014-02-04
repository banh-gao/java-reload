package com.github.reload.message.errors;

import com.github.reload.message.errors.Error.ErrorType;

/**
 * Indicates that the request times out
 * 
 * @author Daniel Zozin <zdenial@gmx.com>
 * 
 */
public class RequestTimeoutException extends ErrorMessageException {

	public RequestTimeoutException(String message) {
		super(new Error(ErrorType.REQUEST_TIMEOUT, message));
	}
}
