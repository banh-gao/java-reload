package com.github.reload.net.data;

import com.github.reload.message.errors.Error;

/**
 * An error response to send to the sender node with the cause of the error
 */
public interface ErrorRespose {

	public Error getError();
}
