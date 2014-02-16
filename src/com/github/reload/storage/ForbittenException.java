package com.github.reload.storage;

import com.github.reload.message.errors.Error.ErrorType;

/**
 * Indicates that an action is not permitted
 * 
 * @author Daniel Zozin <zdenial@gmx.com>
 * 
 */
public class ForbittenException extends StorageException {

	public ForbittenException(String message) {
		super(new Error(ErrorType.FORBITTEN, message));
	}
}
