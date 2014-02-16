package com.github.reload.storage;

import com.github.reload.message.errors.ErrorRespose;
import com.github.reload.message.errors.ErrorType;

/**
 * Indicates that an action is not permitted
 * 
 */
public class ForbittenException extends Exception implements ErrorRespose {

	public ForbittenException(String message) {
		super(message);
	}

	@Override
	public ErrorType getErrorType() {
		return ErrorType.FORBITTEN;
	}
}
