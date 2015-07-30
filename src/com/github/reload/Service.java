package com.github.reload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import com.github.reload.Service.OnLoaded;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Service {

	Class<?>[] value();

	public @interface OnLoaded {
	}
}

class ServiceLoader {

	CoreModule coreModule;

	public ServiceLoader(CoreModule coreModule) {
		this.coreModule = coreModule;
	}

	public <T> T getService(Class<T> service) {
		T instance;
		try {
			instance = coreModule.graph.get(service);
		} catch (IllegalArgumentException e) {
			instance = loadService(service);
		}

		return instance;
	}

	private <T> T loadService(Class<T> service) {
		if (!service.isAnnotationPresent(Service.class))
			throw new IllegalArgumentException("Missing @Service annotation");

		loadModules(service);

		T instance = coreModule.graph.get(service);

		coreModule.graph.inject(instance);

		startService(instance);

		return instance;
	}

	/**
	 * Load new service modules in the object graph. The new modules are visible
	 * only to objects that will be injected after this call. Objects that were
	 * already injected cannot access the new service.
	 * 
	 * @param clazz
	 */
	private void loadModules(Class<?> clazz) {
		Service ann = clazz.getAnnotation(Service.class);
		coreModule.loadModules(ann.value());
	}

	private void startService(Object service) {
		for (Method m : service.getClass().getMethods()) {
			if (m.isAnnotationPresent(OnLoaded.class)) {
				try {
					m.invoke(service);
				} catch (IllegalAccessException | IllegalArgumentException
						| InvocationTargetException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
