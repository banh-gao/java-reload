package com.github.reload.net.pipeline.handlers;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import com.github.reload.net.pipeline.encoders.Message;
import com.google.common.eventbus.EventBus;

/**
 * Message handler at the end of the input pipeline that dispatches incoming
 * RELOAD messages to the application components through the message bus
 */
public class MessageDispatcher extends ChannelInboundHandlerAdapter {

	private final Logger l = Logger.getRootLogger();
	private EventBus msgBus;

	public MessageDispatcher(EventBus msgBus) {
		this.msgBus = msgBus;
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		Message message = (Message) msg;

		l.log(Level.DEBUG, "Message received: " + message);

		msgBus.post(message);
	}
}