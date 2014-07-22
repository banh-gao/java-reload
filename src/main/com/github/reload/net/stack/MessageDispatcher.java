package com.github.reload.net.stack;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import com.github.reload.net.MessageRouter;
import com.github.reload.net.encoders.Message;
import com.github.reload.net.encoders.content.ContentType;

/**
 * Dispatch incoming messages to a proper request handler in a separate thread.
 * The handler is chosen among the registered ones based on the RELOAD message
 * content type.
 * If
 * RELOAD messages to the application components through the message bus
 */
@Sharable
public class MessageDispatcher extends ChannelInboundHandlerAdapter {

	private final Logger l = Logger.getRootLogger();

	private final Executor handlerExecutor;
	private final MessageRouter router;

	private Map<ContentType, RequestHandlerMethod> handlers = new HashMap<ContentType, RequestHandlerMethod>(ContentType.values().length);

	public MessageDispatcher(Executor handlerExecutor, MessageRouter router) {
		this.handlerExecutor = handlerExecutor;
		this.router = router;

		// Handler used to process messages not catched by other handlers
		registerHandler(new Object() {

			@MessageHandler(ContentType.UNKNOWN)
			void handlerUnknown(Message msg) {
				l.warn(String.format("No handler registered for message %#h of type %s", msg.getHeader().getTransactionId(), msg.getContent().getType()));
			}
		});
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		handlerExecutor.execute(new HandlerTask((Message) msg));
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		Logger.getRootLogger().warn(cause.getMessage(), cause);
	}

	public void registerHandler(Object obj) {
		boolean isAnnotationPresent = false;
		for (Method m : obj.getClass().getDeclaredMethods()) {
			MessageHandler ann = m.getAnnotation(MessageHandler.class);

			if (ann == null)
				continue;

			if (!checkMethodSignature(m)) {
				throw new IllegalArgumentException(String.format("Invalid signature for annotated method %s in class %s", m.getName(), m.getDeclaringClass().getCanonicalName()));
			}

			handlers.put(ann.value(), new RequestHandlerMethod(obj, m));
			isAnnotationPresent = true;
		}

		if (!isAnnotationPresent)
			l.warn(String.format("No method annotated as RequestHandler in the given object of type %s", obj.getClass().getCanonicalName()));
	}

	private boolean checkMethodSignature(Method m) {
		Class<?>[] parms = m.getParameterTypes();
		if (parms.length != 1)
			return false;

		return Message.class.isAssignableFrom(parms[0]);
	}

	/**
	 * Determine the message type and execute the proper message handler
	 */
	private class HandlerTask implements Runnable {

		private final Message message;

		public HandlerTask(Message message) {
			this.message = message;
		}

		@Override
		public void run() {

			ContentType type = message.getContent().getType();

			if (type.isAnswer()) {
				l.log(Level.DEBUG, String.format("Handling incoming answer %#x", message.getHeader().getTransactionId()));
				router.handleAnswer(message);
			}

			RequestHandlerMethod handler = handlers.get(type);

			if (handler == null)
				handler = handlers.get(ContentType.UNKNOWN);
			else
				l.log(Level.DEBUG, String.format("Handling incoming request %#x", message.getHeader().getTransactionId()));

			try {
				handler.callHandler(message);
			} catch (IllegalAccessException | IllegalArgumentException
					| InvocationTargetException e) {
				e.printStackTrace();
			}

		}
	}

	private static class RequestHandlerMethod {

		private final Object obj;
		private final Method handler;

		public RequestHandlerMethod(Object obj, Method handler) {
			this.obj = obj;
			this.handler = handler;
		}

		public void callHandler(Message request) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
			handler.setAccessible(true);
			handler.invoke(obj, request);
		}
	}

	/**
	 * Use this annotation to indicate a method that will handle a particular
	 * RELOAD message.
	 * The method will be called when a message of the given type is
	 * received. The method must accept an argument of type {@Message}
	 * otherwise a {@link IllegalArgumentException} will be thrown upon
	 * object registration.
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	public @interface MessageHandler {

		ContentType value();
	}
}