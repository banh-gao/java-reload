package com.github.reload.storage.errors;

import com.github.reload.message.errors.ErrorRespose;
import com.github.reload.message.errors.ErrorType;

/**
 * Indicates that the data value is older than the current stored value
 * 
 */
public class DataTooOldException extends Exception implements ErrorRespose {

	public DataTooOldException(String message) {
		super(message);
	}

	@Override
	public ErrorType getErrorType() {
		return ErrorType.DATA_TOO_OLD;
	}
}
