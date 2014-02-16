package com.github.reload.storage.errors;

import net.sf.jReload.overlay.errors.Error;
import net.sf.jReload.overlay.errors.Error.ErrorType;

/**
 * Indicates that the data value is older than the current stored value
 * 
 * @author Daniel Zozin <zdenial@gmx.com>
 * 
 */
public class DataTooOldException extends StorageException {

	public DataTooOldException(String message) {
		super(message);
	}

	@Override
	public Error getError() {
		return new Error(ErrorType.DATA_TOO_OLD, getMessage());
	}
}
