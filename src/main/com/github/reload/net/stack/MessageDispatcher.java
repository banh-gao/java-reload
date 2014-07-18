package com.github.reload.net.stack;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import com.github.reload.net.MessageBus;
import com.github.reload.net.encoders.Message;

/**
 * Message handler at the end of the input pipeline that dispatches incoming
 * RELOAD messages to the application components through the message bus
 */
public class MessageDispatcher extends ChannelInboundHandlerAdapter {

	private final Logger l = Logger.getRootLogger();
	private MessageBus msgBus;

	public MessageDispatcher(MessageBus msgBus) {
		this.msgBus = msgBus;
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		Message message = (Message) msg;
		l.log(Level.DEBUG, String.format("Dispatching incoming message %#x on application bus...", message.getHeader().getTransactionId()));
		msgBus.post(message);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		Logger.getRootLogger().warn(cause.getMessage(), cause);
	}
}