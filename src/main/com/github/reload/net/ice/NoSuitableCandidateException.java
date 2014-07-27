package com.github.reload.net.ice;

/**
 * Indicates that an ice candidate cannot be reached
 * 
 */
public class NoSuitableCandidateException extends Exception {

	public NoSuitableCandidateException(String message) {
		super(message);
	}

	public NoSuitableCandidateException() {
	}
}
