package com.github.reload.net.connections;

import com.github.reload.Components.MessageHandler;
import com.github.reload.net.encoders.Message;
import com.github.reload.net.encoders.content.ContentType;

/**
 * Handles connection related request messages
 */
public class ConnectionMessageHandler {

	@MessageHandler(ContentType.ATTACH_REQ)
	public void handleAttachRequest(Message req) {
		// TODO
	}

	@MessageHandler(ContentType.LEAVE_REQ)
	public void handleLeaveRequest(Message req) {
		// TODO
	}
}
