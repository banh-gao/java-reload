package com.github.reload.components;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.apache.log4j.Logger;
import com.github.reload.net.encoders.Message;
import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.Maps;
import com.google.common.collect.MutableClassToInstanceMap;
import com.google.common.eventbus.DeadEvent;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

public class ComponentsContext {

	private final ComponentsRepository repo;

	private final ClassToInstanceMap<Object> loadedComponents = MutableClassToInstanceMap.create(new LinkedHashMap<Class<? extends Object>, Object>());

	private final Map<Class<?>, Integer> componentsStatus = Maps.newLinkedHashMap();

	private final MessageHandlersManager msgHandlerMgr;

	private final EventBus eventBus;

	private final Executor defaultExecutor = Executors.newSingleThreadExecutor();

	private static final int STATUS_LOADED = 1;
	private static final int STATUS_STARTED = 2;
	private static final int STATUS_STOPPED = 3;
	private static final int STATUS_UNLOADED = 4;

	public static ComponentsContext newInstance() {
		return new ComponentsContext();
	}

	private Class<? extends Annotation> toAnnotation(int status) {
		switch (status) {
			case STATUS_LOADED :
				return CompLoaded.class;
			case STATUS_STARTED :
				return CompStart.class;
			case STATUS_STOPPED :
				return CompStop.class;
			case STATUS_UNLOADED :
				return CompUnloaded.class;
		}

		throw new IllegalStateException();
	}

	public ComponentsContext() {
		repo = ComponentsRepository.getInstance();
		msgHandlerMgr = new MessageHandlersManager();
		eventBus = new EventBus();
		eventBus.register(this);
	}

	@Subscribe
	public void handleDeadEvents(DeadEvent ev) {
		Logger.getRootLogger().debug("Ignored event " + ev.getEvent());
	}

	public <T> void set(Class<T> compBaseClazz, T comp) {
		loadedComponents.put(compBaseClazz, comp);

		setComponentStatus(compBaseClazz, STATUS_LOADED);
	}

	private void setComponentStatus(Class<?> compBaseClazz, int status) {

		Class<? extends Annotation> annotation = toAnnotation(status);

		componentsStatus.put(compBaseClazz, status);
	}

	/**
	 * Call objects trigger methods annotated with the given annotation. Trigger
	 * methods in parent classes will be called from the base class down to the
	 * current object runtime class.
	 */
	private void triggerStatusMethod(Class<?> clazz, Object obj, Class<? extends Annotation> annotation) {

		// Recursively call parent classes trigger methods before calling object
		// runtime class methods
		if (clazz.getSuperclass() != null) {
			triggerStatusMethod(clazz.getSuperclass(), obj, annotation);
		}

		for (Method m : clazz.getDeclaredMethods()) {
			if (m.isAnnotationPresent(annotation)) {
				try {
					m.setAccessible(true);
					m.invoke(obj);
				} catch (IllegalArgumentException | IllegalAccessException
						| InvocationTargetException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private int getComponentStatus(Class<?> compBaseClazz) {
		return componentsStatus.get(compBaseClazz);
	}

	public void stopComponents() {
		for (Class<?> compBaseClazz : loadedComponents.keySet()) {
			stopComponent(compBaseClazz);
		}
	}

	public boolean stopComponent(Class<?> compBaseClazz) {
		if (getComponentStatus(compBaseClazz) >= STATUS_STOPPED)
			return false;

		return true;
	}

	public void unloadComponent(Class<?> compBaseClazz) {
		stopComponent(compBaseClazz);
		loadedComponents.remove(compBaseClazz);
		componentsStatus.remove(compBaseClazz);
		setComponentStatus(compBaseClazz, STATUS_UNLOADED);
	}

	public void execute(Runnable command) {
		defaultExecutor.execute(command);
	}

	public void handleMessage(final Message msg) {
		defaultExecutor.execute(new Runnable() {

			@Override
			public void run() {
				msgHandlerMgr.handle(msg);
			}
		});
	}

	public void postEvent(Object event) {
		eventBus.post(event);
	}

	/**
	 * The annotated method will be called when the component has associated
	 * with a context.
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	public @interface CompLoaded {

	}

	/**
	 * The annotated method will be called when the component can start to
	 * operate.
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	public @interface CompStart {

	}

	/**
	 * The annotated method will be called when the component have to stop its
	 * internal operations.
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	public @interface CompStop {

	}

	/**
	 * The annotated method will be called when the resources allocated by the
	 * component has to be deallocated.
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	public @interface CompDeinit {

	}

	/**
	 * The annotated method will be called when the component has associated
	 * with a context.
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	public @interface CompUnloaded {

	}

	public <T> T getService(ServiceIdentifier<T> serviceId) {
		return serviceId.getService(this);
	}

	public static class ServiceIdentifier<T> {

		private final Class<?> compBaseClazz;

		public ServiceIdentifier(Class<?> compBaseClazz) {
			this.compBaseClazz = compBaseClazz;
			ComponentsRepository.register(compBaseClazz);
		}

		@SuppressWarnings("unchecked")
		T getService(ComponentsContext ctx) {
			for (Method m : cmp.getClass().getDeclaredMethods()) {
				if (!m.isAnnotationPresent(Service.class)) {
					continue;
				}

				try {
					m.setAccessible(true);
					return (T) m.invoke(cmp);
				} catch (IllegalAccessException | IllegalArgumentException
						| InvocationTargetException e) {
					throw new RuntimeException(e);
				}
			}
			throw new UnsupportedOperationException(String.format("No exposed service for component %s", cmp.getClass().getCanonicalName()));
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((compBaseClazz == null) ? 0 : compBaseClazz.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			ServiceIdentifier<?> other = (ServiceIdentifier<?>) obj;
			if (compBaseClazz == null) {
				if (other.compBaseClazz != null)
					return false;
			} else if (!compBaseClazz.equals(other.compBaseClazz))
				return false;
			return true;
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
}
