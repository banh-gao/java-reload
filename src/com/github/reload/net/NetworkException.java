package com.github.reload.net;

import java.io.IOException;

/**
 * Indicates a problem in the overlay network
 * 
 */
public class NetworkException extends IOException {

	public NetworkException(Throwable e) {
		super(e);
	}

	public NetworkException(String message) {
		super(message);
	}

}
