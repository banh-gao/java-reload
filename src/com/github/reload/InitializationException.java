package com.github.reload;

/**
 * Indicates an error in the RELOAD peer initialization
 * 
 */
public class InitializationException extends Exception {

	public InitializationException(Throwable cause) {
		super(cause);
	}

	public InitializationException(String message) {
		super(message);
	}

}
