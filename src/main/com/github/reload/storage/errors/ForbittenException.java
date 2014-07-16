package com.github.reload.storage.errors;

import com.github.reload.net.encoders.content.errors.ErrorRespose;
import com.github.reload.net.encoders.content.errors.ErrorType;

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
