package com.github.reload.net.stack;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import java.util.concurrent.Executor;
import javax.inject.Inject;
import javax.inject.Named;
import org.apache.log4j.Logger;
import com.github.reload.components.MessageHandlersManager;
import com.github.reload.net.encoders.Message;

/**
 * Dispatch incoming messages to a proper handler in a separate thread.
 * The handler is chosen among the registered components ones based on the
 * RELOAD message content type.
 */
@Sharable
public class MessageDispatcher extends ChannelInboundHandlerAdapter {

	@Inject
	@Named("packetLooper")
	Executor packetLooper;

	@Inject
	MessageHandlersManager msgHandlers;

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		packetLooper.execute(new Runnable() {

			@Override
			public void run() {
				msgHandlers.handle((Message) msg);
			}
		});
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		Logger.getRootLogger().warn(cause.getMessage(), cause);
	}

}