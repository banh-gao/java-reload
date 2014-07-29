package com.github.reload.components;

import java.lang.annotation.Annotation;
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
import org.apache.log4j.Logger;
import com.github.reload.components.ComponentsRepository.Component;
import com.github.reload.net.encoders.Message;
import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.Maps;
import com.google.common.collect.MutableClassToInstanceMap;

public class ComponentsContext {

	private static final Logger l = Logger.getRootLogger();

	private static ComponentsContext instance;

	private final ComponentsRepository repo;

	private final ClassToInstanceMap<Object> loadedComponents = MutableClassToInstanceMap.create();

	private final Map<Class<?>, CompStatus> componentsStatus = Maps.newHashMap();

	private final MessageHandlersManager msgHandlerMgr;

	private Executor defaultExecutor = Executors.newSingleThreadExecutor();

	public static ComponentsContext getDefault() {
		if (instance == null) {
			instance = new ComponentsContext();
		}
		return instance;
	}

	private ComponentsContext() {
		repo = ComponentsRepository.getInstance();
		msgHandlerMgr = new MessageHandlersManager();
	}

	public <T> void set(Class<T> compBaseClazz, T comp) {
		loadedComponents.put(compBaseClazz, comp);
		componentsStatus.put(compBaseClazz, CompStatus.LOADED);

		injectComponents(comp);
		setComponentStatus(compBaseClazz, CompStatus.INITIALIZED);
	}

	private void setComponentStatus(Class<?> compBaseClazz, CompStatus status) {

		Class<? extends Annotation> annotation = status.statusAnnotation;

		Object c = get(compBaseClazz);

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

	private CompStatus getComponentStatus(Class<?> compBaseClazz) {
		return componentsStatus.get(compBaseClazz);
	}

	@SuppressWarnings("unchecked")
	public <T> T get(Class<T> compBaseClazz) {
		if (!loadedComponents.containsKey(compBaseClazz)) {
			T comp = repo.newComponent(compBaseClazz);
			set(compBaseClazz, comp);
		}
		return (T) loadedComponents.get(compBaseClazz);
	}

	public boolean startComponent(Class<?> compBaseClazz) {
		Object cmp = get(compBaseClazz);

		if (getComponentStatus(compBaseClazz) != CompStatus.LOADED)
			return false;

		setComponentStatus(compBaseClazz, CompStatus.STARTED);
		msgHandlerMgr.registerMessageHandler(cmp);
		return true;
	}

	public void startComponents() {
		for (Class<?> compBaseClazz : loadedComponents.keySet()) {
			startComponent(compBaseClazz);
		}
	}

	public boolean stopComponent(Class<?> compBaseClazz) {
		if (getComponentStatus(compBaseClazz) != CompStatus.STARTED)
			return false;

		Object cmp = get(compBaseClazz);
		setComponentStatus(compBaseClazz, CompStatus.STOPPED);
		msgHandlerMgr.unregisterMessageHandler(cmp);
		return true;
	}

	public void stopComponents() {
		for (Class<?> compBaseClazz : loadedComponents.keySet())
			stopComponent(compBaseClazz);
	}

	public void unloadComponent(Class<?> compBaseClazz) {
		stopComponent(compBaseClazz);
		loadedComponents.remove(compBaseClazz);
		componentsStatus.remove(compBaseClazz);
		setComponentStatus(compBaseClazz, CompStatus.UNLOADED);
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

	private void injectComponents(Object c) {
		for (Field f : c.getClass().getDeclaredFields()) {
			Component cmp = f.getAnnotation(Component.class);
			if (cmp != null) {
				try {
					f.setAccessible(true);

					Class<?> compBaseClazz = f.getType();

					Object obj = (compBaseClazz.equals(this.getClass())) ? this : get(compBaseClazz);

					if (obj != null)
						f.set(c, obj);
					else
						l.warn(String.format("Missing component %s required by %s", compBaseClazz.getCanonicalName(), c.getClass().getCanonicalName()));
				} catch (IllegalArgumentException | IllegalAccessException e) {
					e.printStackTrace();
				}
			}
		}
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
	 * The annotated method will be called when initialization has to take
	 * place.
	 * Also at this point all the component fields annotated with
	 * {@link Component} have been injected.
	 * Note than at this point some injected components may yet still not have
	 * been initialized.
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	public @interface CompInit {

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
		}

		@SuppressWarnings("unchecked")
		T getService(ComponentsContext ctx) {
			Object cmp = ctx.get(compBaseClazz);

			ctx.startComponent(compBaseClazz);

			for (Method m : cmp.getClass().getDeclaredMethods()) {
				if (!m.isAnnotationPresent(Service.class))
					continue;

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

	private enum CompStatus {
		LOADED(CompLoaded.class),
		INITIALIZED(CompInit.class),
		STARTED(CompStart.class),
		STOPPED(CompStop.class),
		UNLOADED(CompUnloaded.class);

		final Class<? extends Annotation> statusAnnotation;

		private CompStatus(Class<? extends Annotation> statusAnnotation) {
			this.statusAnnotation = statusAnnotation;
		}
	}
}
