package com.github.reload.net;

import com.github.reload.net.data.ForwardMessage;

public class MessageTransmitter {

	/**
	 * Start the transmission of the message to the neighbor nodes. Since the
	 * transmission is performed asyncronously, this method returns immediately
	 * and the returned TransmissionFuture can be used to control the
	 * transmission status of the message.
	 * 
	 * @param message
	 * @return
	 */
	public TransmissionFuture sendMessage(ForwardMessage message) {
		// TODO: forward message to neighbors and update transmissionFuture with
		// transmission status updates
		return null;
	}
}
