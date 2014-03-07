package com.github.reload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.util.Map;
import com.github.reload.net.data.Message;
import com.google.common.collect.Maps;
import com.google.common.eventbus.EventBus;

/**
 * Application context where all application components are registered.
 * Every registered component is connected to the incoming message bus so that
 * they can receive incoming reload messages.
 */
public class Context {

	private final Map<Class<? extends Component>, Component> components = Maps.newHashMap();

	private final EventBus messageBus;

	public Context(EventBus messageBus) {
		this.messageBus = messageBus;
	}

	@SuppressWarnings("unchecked")
	public <T extends Component> T setComponent(Class<T> clazz, T cmp) {
		messageBus.register(cmp);
		T oldCmp = (T) components.put(clazz, cmp);
		if (oldCmp != null)
			messageBus.unregister(oldCmp);
		return oldCmp;
	}

	public void initComponents() {
		for (Component c : components.values())
			injectComponents(c);

		for (Component c : components.values())
			c.compStart();
	}

	private void injectComponents(Component c) {
		for (Field f : c.getClass().getFields()) {
			if (f.isAnnotationPresent(CtxComponent.class)) {

				if (!f.getType().isAssignableFrom(Component.class)) {
					throw new ClassCastException("Component field type must be compatible with the component type");
				} else {
					@SuppressWarnings("unchecked")
					Class<? extends Component> fldType = (Class<? extends Component>) f.getType();
					try {
						f.set(c, getComponent(fldType));
					} catch (IllegalArgumentException | IllegalAccessException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	public <T extends Component> T getComponent(Class<T> key) {
		return (T) components.get(key);
	}

	@SuppressWarnings("unchecked")
	public <T extends Component> T unsetComponent(Class<T> key) {
		T cmp = (T) components.remove(key);
		if (cmp != null)
			messageBus.unregister(cmp);
		return cmp;
	}

	public void postMessage(Message message) {
		messageBus.post(message);
	}

	public interface Component {

		/**
		 * This method will be called after all components have been loaded into
		 * the context. Also all the component fields annotated with
		 * {@link CtxComponent} have been loaded.
		 * Note than at this point some components may yet still not have been
		 * initialized.
		 * 
		 * @param context
		 */
		public void compStart();
	}
	/**
	 * Use this annotation to indicate a field where a component has to be
	 * injected.
	 * If the field type must be compatible with {@link Component}, otherwise a
	 * {@link ClassCastException} will be thrown
	 * 
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.FIELD)
	public @interface CtxComponent {
	}
}