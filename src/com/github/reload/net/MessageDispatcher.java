package com.github.reload.net;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import java.util.concurrent.Executor;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import com.github.reload.Context;
import com.github.reload.message.errors.ErrorRespose;
import com.github.reload.net.data.Message;
import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.SubscriberExceptionContext;
import com.google.common.eventbus.SubscriberExceptionHandler;

/**
 * Message handler that dispatch incoming messages to receiver worker threads
 */
public class MessageDispatcher extends ChannelInboundHandlerAdapter implements SubscriberExceptionHandler {

	private final Logger l = Logger.getRootLogger();

	private final EventBus messageBus;

	public MessageDispatcher(Context context, Executor msgHandlers) {
		messageBus = new AsyncEventBus(msgHandlers, this);
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		Message message = (Message) msg;

		l.log(Level.DEBUG, "Message received: " + message);

		messageBus.post(message);
	}

	public void registerReceiver(Object receiver) {
		messageBus.register(receiver);
	}

	@Override
	public void handleException(Throwable exception, SubscriberExceptionContext context) {
		if (exception instanceof ErrorRespose) {
			ErrorRespose resp = (ErrorRespose) exception;
			// TODO: send error response message
			l.log(Level.DEBUG, exception.getMessage(), exception);
		} else {
			l.log(Level.WARN, exception.getMessage(), exception);
		}
	}
}