package com.github.reload.conf;

/**
 * Indicates a problem in the overlay configuration
 * 
 */
public class ConfigurationException extends Exception {

	public ConfigurationException(String message) {
		super(message);
	}

	public ConfigurationException(Throwable e) {
		super(e);
	}
}
