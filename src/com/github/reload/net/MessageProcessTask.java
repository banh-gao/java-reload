package com.github.reload.net;

import com.github.reload.net.data.Message;

/**
 * The processing task to be performed for an incoming message
 */
public class MessageProcessTask implements Runnable {

	private final Message message;

	public MessageProcessTask(Message message) {
		this.message = message;
	}

	@Override
	public void run() {
		System.out.println("Processing incoming message: " + message);
		System.out.println(message.getContent());
	}
}
