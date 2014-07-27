package com.github.reload;

import java.lang.annotation.Annotation;
import java.lang.annotation.AnnotationFormatError;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import com.github.reload.net.encoders.Message;
import com.github.reload.net.encoders.content.ContentType;
import com.google.common.collect.Maps;

/**
 * Application context where all application components are registered.
 */
public class Components {

	private static final Logger l = Logger.getRootLogger();

	private static final Map<String, Object> components = Maps.newLinkedHashMap();

	private static final Map<ContentType, MessageHandlerMethod> messageHandlers = Maps.newHashMapWithExpectedSize(ContentType.values().length);
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

		if (cmpAnn == null)
			throw new IllegalArgumentException(String.format("Component annotation for object %s not found", c.getClass().getCanonicalName()));

		Object oldComp = components.put(cmpAnn.value(), c);
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
			Component cmp = f.getAnnotation(Component.class);
			if (cmp != null) {
				try {
					f.setAccessible(true);

					String name = cmp.value();

					if (name.isEmpty()) {
						Component fldTypeAnn = f.getType().getAnnotation(Component.class);
						if (fldTypeAnn != null)
							name = fldTypeAnn.value();
					}

					if (name.isEmpty())
						throw new AnnotationFormatError(String.format("Component for injection in field %s of %s not found", f.getName(), f.getDeclaringClass().getCanonicalName()));

					Object obj = get(name);
					if (obj != null)
						f.set(c, obj);
					else
						l.warn(String.format("Missing component %s required by %s", name, c.getClass().getCanonicalName()));
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

	public static Object get(String component) {
		return components.get(component);
	}

	public static <T> T getService(ServiceIdentifier<T> serviceId) {
		return serviceId.loadService();
	}

	public static class ServiceIdentifier<T> {

		private final String compName;

		public ServiceIdentifier(String compName) {
			this.compName = compName;
		}

		@SuppressWarnings("unchecked")
		T loadService() {
			Object cmp = get(compName);
			for (Method m : cmp.getClass().getDeclaredMethods()) {
				if (!m.isAnnotationPresent(Service.class))
					continue;

				try {
					return (T) m.invoke(cmp);
				} catch (IllegalAccessException | IllegalArgumentException
						| InvocationTargetException e) {
					e.printStackTrace();
				}
			}
			return null;
		}
	}

	/**
	 * The annotated method will be used to expose a service provided by the
	 * component to the library client
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	public @interface Service {
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
	 * Applied on a type, the annotation is used to specify the component name
	 * the
	 * annotated type has to registered
	 * Applied on a field of a registered component it indicates that the
	 * component corresponding to the given component name has to be injected.
	 * 
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target({ElementType.TYPE, ElementType.FIELD})
	public @interface Component {

		String value() default "";
	}

	public static void registerMessageHandler(Object obj) {
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