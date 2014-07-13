package com.github.reload.storage.errors;

import com.github.reload.message.errors.ErrorRespose;
import com.github.reload.message.errors.ErrorType;

/**
 * Indicates that a requested value was not found
 * 
 */
public class NotFoundException extends Exception implements ErrorRespose {

	public NotFoundException(String message) {
		super(message);
	}

	@Override
	public ErrorType getErrorType() {
		return ErrorType.NOT_FOUND;
	}

}
