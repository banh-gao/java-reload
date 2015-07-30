package com.github.reload.components;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.apache.log4j.Logger;
import com.google.common.eventbus.DeadEvent;
import com.google.common.eventbus.Subscribe;

public class ComponentsContext {

	public static ComponentsContext newInstance() {
		return new ComponentsContext();
	}

	@Subscribe
	public void handleDeadEvents(DeadEvent ev) {
		Logger.getRootLogger().debug("Ignored event " + ev.getEvent());
	}

	public void stopComponents() {
		for (Class<?> compBaseClazz : loadedComponents.keySet()) {
			stopComponent(compBaseClazz);
		}
	}

	public void postEvent(Object event) {
		eventBus.post(event);
	}

	/**
	 * The annotated method will be called when the component have to stop its
	 * internal operations.
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	public @interface CompStop {

	}

}
