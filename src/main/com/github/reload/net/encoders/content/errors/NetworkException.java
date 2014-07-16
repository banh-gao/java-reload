package com.github.reload.net.encoders.content.errors;

import java.io.IOException;

/**
 * Indicates a problem in the overlay network
 * 
 * @author Daniel Zozin <zdenial@gmx.com>
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
