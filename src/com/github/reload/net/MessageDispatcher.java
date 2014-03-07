package com.github.reload.net;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import com.github.reload.Component;
import com.github.reload.Context;
import com.github.reload.net.data.Message;

/**
 * Message handler that dispatch incoming messages to receiver worker threads
 */
public class MessageDispatcher extends ChannelInboundHandlerAdapter implements Component {

	private final Logger l = Logger.getRootLogger();
	private Context context;

	@Override
	public void compAdded(Context context) {
		this.context = context;
	}

	@Override
	public void compRemoved(Context context) {

	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		Message message = (Message) msg;

		l.log(Level.DEBUG, "Message received: " + message);

		context.postMessage(message);
	}
}