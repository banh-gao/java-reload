package com.github.reload.message.errors;

import com.github.reload.message.errors.Error.ErrorType;

/**
 * Indicates a problem with some overlay parameter
 * 
 * @author Daniel Zozin <zdenial@gmx.com>
 * 
 */
public class IncompatibleOverlayException extends ErrorMessageException {

	public IncompatibleOverlayException(String message) {
		super(new Error(ErrorType.INCOMPATIBLE_WITH_OVERLAY, message));
	}
}
