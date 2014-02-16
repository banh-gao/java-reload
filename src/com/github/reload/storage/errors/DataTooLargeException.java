package com.github.reload.storage.errors;

import com.github.reload.message.errors.Error.ErrorType;

/**
 * Indicates that the data exceeds the maximum allowed size
 * 
 * @author Daniel Zozin <zdenial@gmx.com>
 * 
 */
public class DataTooLargeException extends StorageException {

	public DataTooLargeException(String message) {
		super(message);
	}

	@Override
	public Error getError() {
		return new Error(ErrorType.DATA_TOO_LARGE, getMessage());
	}

}
