package com.github.reload.storage.errors;

import net.sf.jReload.overlay.errors.Error;
import net.sf.jReload.overlay.errors.Error.ErrorType;

/**
 * Indicates that a requested value was not found
 * 
 * @author Daniel Zozin <zdenial@gmx.com>
 * 
 */
public class NotFoundException extends StorageException {

	public NotFoundException(String message) {
		super(message);
	}

	@Override
	public Error getError() {
		return new Error(ErrorType.NOT_FOUND, getMessage());
	}

}
