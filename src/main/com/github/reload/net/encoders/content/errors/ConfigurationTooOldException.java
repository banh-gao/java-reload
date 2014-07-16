package com.github.reload.net.encoders.content.errors;

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
