package com.github.reload.message.errors;

import com.github.reload.message.errors.Error.ErrorType;

/**
 * Indicates that the configuration sequence is older than the local
 * 
 * @author Daniel Zozin <zdenial@gmx.com>
 * 
 */
public class ConfigurationTooOldException extends ErrorMessageException {

	public ConfigurationTooOldException(String message) {
		super(new Error(ErrorType.CONFIG_TOO_OLD, message));
	}
}
