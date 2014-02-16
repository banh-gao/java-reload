package com.github.reload.message.errors;


/**
 * Indicates that the configuration sequence is older than the local
 * 
 */
public class ConfigurationTooOldException extends Exception implements ErrorRespose {

	public ConfigurationTooOldException(String message) {
		super(message);
	}

	@Override
	public ErrorType getErrorType() {
		return ErrorType.CONFIG_TOO_OLD;
	}
}
