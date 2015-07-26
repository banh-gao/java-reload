package com.github.reload.components;

import java.util.Map;
import com.google.common.collect.Maps;

/**
 * Application context where all application components are registered.
 */
public class ComponentsRepository {

	private static final Map<Class<?>, Class<?>> components = Maps.newConcurrentMap();

	private static ComponentsRepository instance;

	public static ComponentsRepository getInstance() {
		if (instance == null) {
			instance = new ComponentsRepository();
		}

		return instance;
	}

	private ComponentsRepository() {
	}

	public static void register(Class<?> comp) {
	}

	private static void checkDefaultConstructor(Class<?> comp) {
		try {
			// Check for default (no arguments) constructor
			comp.getDeclaredConstructor();
		} catch (NoSuchMethodException e) {
			throw new IllegalArgumentException(String.format("Component %s without a default constructor not allowed", comp.getCanonicalName()));
		}
	}

	public static void unregister(Class<?> comp) {

	}
}