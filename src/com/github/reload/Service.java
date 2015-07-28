package com.github.reload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import dagger.ObjectGraph;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Service {

	Class<?>[] value();
}

class ServiceLoader {

	ObjectGraph graph;

	public ServiceLoader(ObjectGraph graph) {
		this.graph = graph;
	}

	public <T> T getService(Class<T> service) {
		T instance;
		try {
			instance = graph.get(service);
		} catch (IllegalArgumentException e) {
			instance = loadService(service);
		}

		return instance;
	}

	private <T> T loadService(Class<T> service) {
		checkClass(service);

		loadModules(service);

		T instance = graph.get(service);

		graph.inject(instance);

		return instance;
	}

	private void checkClass(Class<?> clazz) {
		if (!clazz.isAnnotationPresent(Service.class))
			throw new IllegalArgumentException("Missing @Service annotation");
		try {
			clazz.getConstructor();
		} catch (NoSuchMethodException e) {
			throw new IllegalArgumentException("The service class must have a no-parameter constructor");
		} catch (SecurityException e) {
			throw new IllegalArgumentException(e);
		}
	}

	private void loadModules(Class<?> clazz) {
		Service ann = clazz.getAnnotation(Service.class);
		for (Class<?> mod : ann.value()) {
			try {
				graph = graph.plus(mod.newInstance());
			} catch (InstantiationException | IllegalAccessException e) {
				throw new IllegalArgumentException(e);
			}
		}
	}
}
