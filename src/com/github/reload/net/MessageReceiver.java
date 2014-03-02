package com.github.reload.net;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import com.github.reload.Context;
import com.github.reload.message.ContentType;
import com.github.reload.net.data.Message;

/**
 * Message handler that dispatch incoming messages to receiver worker threads
 */
public class MessageReceiver extends ChannelInboundHandlerAdapter {

	private final Logger l = Logger.getRootLogger();

	private final Context ctx;
	private final ExecutorService receivingWorkers = Executors.newSingleThreadExecutor();

	public MessageReceiver(Context ctx) {
		this.ctx = ctx;
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		Message message = (Message) msg;

		ContentType cntType = message.getContent().getType();
		MessageProcessor processor = this.ctx.getMessageProcessor(cntType);
		receivingWorkers.submit(new MessageProcessTask(message, processor));
	}

	/**
	 * The processing task to be performed for an incoming message
	 */
	public class MessageProcessTask implements Runnable {

		private final Message message;

		// All answer messages are delivered to the transmitter
		private final MessageProcessor processor;

		public MessageProcessTask(Message message, MessageProcessor processor) {
			this.message = message;
			this.processor = processor;
		}

		@Override
		public void run() {
			l.log(Level.DEBUG, "Processing incoming message: " + message);
			processor.processMessage(message);
		}
	}

	/**
	 * Declare an object able to handle some type of message content
	 */
	public interface MessageProcessor {

		/**
		 * @return the content types this object is able to handle
		 */
		public Set<ContentType> getAcceptedTypes();

		/**
		 * Process the given message
		 * 
		 * @param message
		 */
		public void processMessage(Message message);
	}
}