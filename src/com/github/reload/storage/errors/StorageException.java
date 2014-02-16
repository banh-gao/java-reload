package com.github.reload.storage.errors;

import com.github.reload.message.errors.Error.ErrorType;
import com.github.reload.message.errors.ErrorMessageException;

/**
 * Indicates a problem with the storage functionalities
 * 
 * @author Daniel Zozin <zdenial@gmx.com>
 * 
 */
public class StorageException extends ErrorMessageException {

	public StorageException(String message) {
		super(new Error(ErrorType.INVALID_MESSAGE, message));
	}

	public StorageException(Error error) {
		super(error);
	}
}
