package com.github.reload.message.errors;

/**
 * Indicates that the configuration sequence is newer that the local
 * 
 */
public class ConfigurationTooNewException extends Exception implements ErrorRespose {

	public ConfigurationTooNewException(String message) {
		super(message);
	}

	@Override
	public ErrorType getErrorType() {
		return ErrorType.CONFIG_TOO_NEW;
	}
}
