package com.github.reload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.MutableClassToInstanceMap;

/**
 * Application context where all application components are registered.
 */
public class Components {

	private static final ClassToInstanceMap<Object> components = MutableClassToInstanceMap.create();

	public static void register(Object c) {
		Component cmpAnn = c.getClass().getAnnotation(Component.class);
		Class<? extends Object> clazz = (cmpAnn != null && !cmpAnn.value().equals(Object.class)) ? cmpAnn.value() : c.getClass();

		if (!clazz.isAssignableFrom(c.getClass()))
			throw new IllegalArgumentException(String.format("The specified component class %s has to be a super-type of class %s", clazz, c.getClass()));

		components.put(clazz, c);
	}

	public static void initComponents() {
		for (Object c : components.values())
			injectComponents(c);

		for (Object c : components.values())
			initCompontent(c);
	}

	public static void deinitComponents() {
		for (Object c : components.values())
			deinitCompontent(c);
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

	private static void initCompontent(Object c) {
		for (Method m : c.getClass().getDeclaredMethods()) {
			if (m.isAnnotationPresent(start.class)) {
				try {
					m.invoke(c);
				} catch (IllegalArgumentException | IllegalAccessException
						| InvocationTargetException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private static void deinitCompontent(Object c) {
		for (Method m : c.getClass().getDeclaredMethods()) {
			if (m.isAnnotationPresent(stop.class)) {
				try {
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

	public static void unregister(Class<?> key) {
		components.remove(key);
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
}