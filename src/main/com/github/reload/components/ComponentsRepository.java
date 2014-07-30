package com.github.reload.components;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.NoSuchElementException;
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

		Class<?> baseComp = getCompBaseClass(comp);

		components.put(baseComp, comp);
	}

	private static <T> Class<? super T> getCompBaseClass(Class<T> compClazz) {
		Component cmpAnn = compClazz.getAnnotation(Component.class);

		checkDefaultConstructor(compClazz);

		if (cmpAnn == null)
			throw new IllegalArgumentException(String.format("Given class %s doesn't refer to any base component", compClazz.getCanonicalName()));

		if (!cmpAnn.value().isAssignableFrom(compClazz))
			throw new IllegalArgumentException(String.format("Component implementation %s is not derived from base component %s", compClazz.getCanonicalName(), cmpAnn.value().getCanonicalName()));

		@SuppressWarnings("unchecked")
		Class<? super T> compBaseclass = (Class<? super T>) cmpAnn.value();

		return compBaseclass;
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
		components.remove(getCompBaseClass(comp));
	}

	public <T> T newComponent(Class<T> compBaseClazz) {
		@SuppressWarnings("unchecked")
		Class<? extends T> compClazz = (Class<? extends T>) components.get(compBaseClazz);
		if (compClazz == null)
			throw new NoSuchElementException(String.format("No registered component provides %s", compBaseClazz.getCanonicalName()));
		try {
			@SuppressWarnings("unchecked")
			Constructor<T> constructor = (Constructor<T>) compClazz.getDeclaredConstructor();
			constructor.setAccessible(true);
			return constructor.newInstance();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
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

		Class<?> value() default Object.class;
	}
}