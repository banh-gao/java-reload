package com.github.reload.storage.errors;

import com.github.reload.message.errors.ErrorRespose;
import com.github.reload.message.errors.ErrorType;

/**
 * Indicates that the data exceeds the maximum allowed size
 * 
 */
public class DataTooLargeException extends Exception implements ErrorRespose {

	public DataTooLargeException(String message) {
		super(message);
	}

	@Override
	public ErrorType getErrorType() {
		return ErrorType.DATA_TOO_LARGE;
	}

}
