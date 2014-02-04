package com.github.reload.message.errors;

import com.github.reload.message.errors.Error.ErrorType;

/**
 * Indicates that the configuration sequence is newer that the local
 * 
 * @author Daniel Zozin <zdenial@gmx.com>
 * 
 */
public class ConfigurationTooNewException extends ErrorMessageException {

	public ConfigurationTooNewException(String message) {
		super(new Error(ErrorType.CONFIG_TOO_NEW, message));
	}
}
