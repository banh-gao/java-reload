package com.github.reload.net.handlers;

import io.netty.buffer.ByteBuf;
import com.github.reload.net.data.FrameMessage;

/**
 * Simple Reliability Link level protocol handler
 */
public class SRLinkHandler extends LinkHandler {

	@Override
	protected void read(FrameMessage message) {
		// TODO Auto-generated method stub

	}

	@Override
	protected FrameMessage write(ByteBuf data) {
		// TODO Auto-generated method stub
		return null;
	}

}
