package com.github.reload;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import com.github.reload.net.encoders.Message;
import com.github.reload.net.encoders.content.ContentType;
import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.MutableClassToInstanceMap;

/**
 * Application context where all application components are registered.
 */
public class Components {

	private static final Logger l = Logger.getRootLogger();

	private static final ClassToInstanceMap<Object> components = MutableClassToInstanceMap.create();

	private static final Map<ContentType, MessageHandlerMethod> messageHandlers = new HashMap<ContentType, MessageHandlerMethod>(ContentType.values().length);
	private static MessageHandlerMethod answerHandler;

	private static Executor handlerExecutor = Executors.newSingleThreadExecutor();

	static {
		// Handler used to process messages not catched by other handlers
		registerMessageHandler(new Object() {

			@MessageHandler(ContentType.UNKNOWN)
			void handlerUnknown(Message msg) {
				if (msg.getContent().isRequest())
					l.warn(String.format("No handler registered for message %#x of type %s", msg.getHeader().getTransactionId(), msg.getContent().getType()));
			}
		});
	}

	public static void setHandlerExecutor(Executor handlerExecutor) {
		Components.handlerExecutor = handlerExecutor;
	}

	public static void register(Object c) {
		Component cmpAnn = c.getClass().getAnnotation(Component.class);
		Class<? extends Object> clazz = (cmpAnn != null && !cmpAnn.value().equals(Object.class)) ? cmpAnn.value() : c.getClass();

		if (!clazz.isAssignableFrom(c.getClass()))
			throw new IllegalArgumentException(String.format("The specified component class %s has to be a super-type of class %s", clazz, c.getClass()));

		Object oldComp = components.put(clazz, c);
		if (oldComp != null)
			callStatusMethod(oldComp, unregistered.class);

		callStatusMethod(c, registered.class);
	}

	public static void unregister(Class<?> key) {
		Object oldComp = components.remove(key);

		if (oldComp != null) {
			callStatusMethod(oldComp, unregistered.class);
			unregisterMessageHandler(oldComp);
		}
	}

	public static void initComponents() {
		for (Object c : components.values())
			injectComponents(c);

		for (Object c : components.values())
			callStatusMethod(c, start.class);

		for (Object c : components.values())
			registerMessageHandler(c);
	}

	public static void deinitComponents() {
		for (Object c : components.values())
			callStatusMethod(c, stop.class);
	}

	private static void injectComponents(Object c) {
		for (Field f : c.getClass().getDeclaredFields()) {
			if (f.isAnnotationPresent(Component.class)) {
				try {
					f.setAccessible(true);
					f.set(c, get(f.getType()));
				} catch (IllegalArgumentException | IllegalAccessException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private static void callStatusMethod(Object c, Class<? extends Annotation> annotation) {
		for (Method m : c.getClass().getDeclaredMethods()) {
			if (m.isAnnotationPresent(annotation)) {
				try {
					m.setAccessible(true);
					m.invoke(c);
				} catch (IllegalArgumentException | IllegalAccessException
						| InvocationTargetException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public static <T> T get(Class<T> key) {
		return components.getInstance(key);
	}

	/**
	 * The annotated method will be called when the component has been
	 * registered.
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	public @interface registered {

	}

	/**
	 * The annotated method will be called when the component has been
	 * unregistered.
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	public @interface unregistered {

	}

	/**
	 * The annotated method will be called when initialization has to take
	 * place.
	 * Also at this point all the component fields annotated with
	 * {@link Component} have been injected.
	 * Note than at this point some components may yet still not have been
	 * initialized.
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	public @interface start {

	}

	/**
	 * The annotated method will be called when deinitialization has to take
	 * place.
	 * Note than at this point some components may have been already
	 * deinitialized.
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	public @interface stop {

	}

	/**
	 * Applied on a type, the annotation can be used to specify the class the
	 * annotated type has to registered instead of using the component effective
	 * class (it must be a super-type of the annotated type)
	 * Applied on a field of a registered component it indicates that the
	 * component corresponding to the field type has to be injected.
	 * 
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target({ElementType.TYPE, ElementType.FIELD})
	public @interface Component {

		Class<?> value() default Object.class;
	}

	private static void registerMessageHandler(Object obj) {
		for (Method m : obj.getClass().getDeclaredMethods()) {
			MessageHandler ann = m.getAnnotation(MessageHandler.class);

			if (ann == null)
				continue;

			if (!checkHandlerMethodSignature(m)) {
				throw new IllegalArgumentException(String.format("Invalid signature for annotated method %s in class %s", m.getName(), m.getDeclaringClass().getCanonicalName()));
			}

			if (ann.handleAnswers())
				answerHandler = new MessageHandlerMethod(obj, m);
			else
				messageHandlers.put(ann.value(), new MessageHandlerMethod(obj, m));
		}
	}

	private static void unregisterMessageHandler(Object obj) {
		for (Method m : obj.getClass().getDeclaredMethods()) {
			MessageHandler ann = m.getAnnotation(MessageHandler.class);

			if (ann == null)
				continue;

			messageHandlers.remove(ann.value());

			if (ann.handleAnswers()) {
				answerHandler = null;
			}
		}
	}

	private static boolean checkHandlerMethodSignature(Method m) {
		Class<?>[] parms = m.getParameterTypes();
		if (parms.length != 1)
			return false;

		return Message.class.isAssignableFrom(parms[0]);
	}

	private static class MessageHandlerMethod {

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

	public static void callMessageHandler(Message message) {
		handlerExecutor.execute(new HandlerTask(message));
	}

	/**
	 * Determine the message type and execute the proper message handler
	 */
	private static class HandlerTask implements Runnable {

		private final Message message;

		public HandlerTask(Message message) {
			this.message = message;
		}

		@Override
		public void run() {
			ContentType type = message.getContent().getType();

			MessageHandlerMethod handler = messageHandlers.get(type);

			if (handler == null) {
				if (type.isAnswer() && answerHandler != null)
					handler = answerHandler;
				else
					handler = messageHandlers.get(ContentType.UNKNOWN);
			} else {
				l.log(Level.DEBUG, String.format("Handling incoming message %#x of type %s using %s", message.getHeader().getTransactionId(), type, handler));
			}

			try {
				handler.callHandler(message);
			} catch (IllegalAccessException | IllegalArgumentException
					| InvocationTargetException e) {
				e.printStackTrace();
			}
		}
	}

}