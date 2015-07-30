package com.github.reload.routing;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.apache.log4j.Logger;
import com.github.reload.net.codecs.Message;
import com.github.reload.net.codecs.content.ContentType;
import com.google.common.collect.Maps;
import com.google.common.eventbus.DeadEvent;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

@Singleton
public class MessageHandlers {

	private static final Logger l = Logger.getRootLogger();

	private final Map<ContentType, MessageHandlerMethod> messageHandlers = Maps.newHashMapWithExpectedSize(ContentType.values().length);
	private MessageHandlerMethod answerHandler;

	EventBus eventBus;

	@Inject
	public MessageHandlers(EventBus eventBus) {
		this.eventBus = eventBus;
		eventBus.register(this);
	}

	public void register(Object obj) {
		for (Method m : obj.getClass().getDeclaredMethods()) {
			MessageHandler ann = m.getAnnotation(MessageHandler.class);

			if (ann == null) {
				continue;
			}

			if (!checkHandlerMethodSignature(m))
				throw new IllegalArgumentException(String.format("Invalid signature for annotated method %s in class %s", m.getName(), m.getDeclaringClass().getCanonicalName()));

			if (ann.handleAnswers()) {
				answerHandler = new MessageHandlerMethod(obj, m);
			} else {
				messageHandlers.put(ann.value(), new MessageHandlerMethod(obj, m));
			}
		}
	}

	public void unregister(Object obj) {
		for (Method m : obj.getClass().getDeclaredMethods()) {
			MessageHandler ann = m.getAnnotation(MessageHandler.class);

			if (ann == null) {
				continue;
			}

			messageHandlers.remove(ann.value());

			if (ann.handleAnswers()) {
				answerHandler = null;
			}
		}
	}

	private boolean checkHandlerMethodSignature(Method m) {
		Class<?>[] parms = m.getParameterTypes();
		if (parms.length != 1)
			return false;

		return Message.class.isAssignableFrom(parms[0]);
	}

	private class MessageHandlerMethod {

		private final Object obj;
		private final Method handler;

		public MessageHandlerMethod(Object obj, Method handler) {
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

		boolean handleAnswers() default false;
	}

	public void handle(Message message) {
		ContentType type = message.getContent().getType();

		MessageHandlerMethod handler = messageHandlers.get(type);

		if (handler == null) {
			if (type.isAnswer() && answerHandler != null) {
				handler = answerHandler;
				l.debug(String.format("Processing %s message %#x with answer handler %s.%s()", type, message.getHeader().getTransactionId(), handler.obj.getClass().getCanonicalName(), handler.handler.getName()));
			} else {
				l.debug(String.format("Swallowed %s message %#x (No registered message handler)", type, message.getHeader().getTransactionId()));
				return;
			}
		} else {
			l.debug(String.format("Processing %s message %#x with handler %s.%s()", type, message.getHeader().getTransactionId(), handler.obj.getClass().getCanonicalName(), handler.handler.getName()));
		}

		try {
			handler.callHandler(message);
		} catch (IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e) {
			e.printStackTrace();
		}
	}

	@Subscribe
	public void handleDeadEvents(DeadEvent ev) {
		Logger.getRootLogger().trace("Ignored event " + ev.getEvent());
	}
}
