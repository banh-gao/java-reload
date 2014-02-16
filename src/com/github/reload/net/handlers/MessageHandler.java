package com.github.reload.net.handlers;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import com.github.reload.net.MessageProcessTask;
import com.github.reload.net.data.Message;

/**
 * Message handler that dispatch incoming messages to receiver workers
 */
public class MessageHandler extends ChannelInboundHandlerAdapter {

	private final ExecutorService receivingWorkers = Executors.newSingleThreadExecutor();

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		Message message = (Message) msg;
		receivingWorkers.submit(new MessageProcessTask(message));
	}
}