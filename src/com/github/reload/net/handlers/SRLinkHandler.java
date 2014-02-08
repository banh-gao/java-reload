package com.github.reload.net.handlers;

import io.netty.buffer.ByteBuf;
import com.github.reload.net.data.FramedMessage;
import com.github.reload.net.data.FramedMessage.FramedData;

/**
 * Simple Reliability Link level protocol handler
 */
public class SRLinkHandler extends LinkHandler {

	@Override
	protected void handleReceived(FramedMessage message) {
		// TODO: implement link layer Simple Reliability
	}

	@Override
	protected FramedData getDataFrame(ByteBuf payload) {
		// TODO
		return new FramedData(1, payload);
	}

}
