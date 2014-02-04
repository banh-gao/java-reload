package com.github.reload.message.errors;

import com.github.reload.message.errors.Error.ErrorType;

/**
 * Indicates an already in progress operation
 * 
 * @author Daniel Zozin <zdenial@gmx.com>
 * 
 */
public class InProgressException extends ErrorMessageException {

	public InProgressException(String info) {
		super(new Error(ErrorType.IN_PROGRESS, info));
	}
}
