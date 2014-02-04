package com.github.reload.net;

import com.github.reload.message.Message;

/**
 * Main class representing the processing task to be performed for an incoming
 * message
 */
public class MessageProcessTask implements Runnable {

	private final Message message;

	public MessageProcessTask(Message message) {
		this.message = message;
	}

	@Override
	public void run() {
		// TODO: process the incoming message
	}
}
