package com.github.reload;

import java.util.ArrayList;
import java.util.List;
import javax.naming.ConfigurationException;
import com.github.reload.conf.Configuration;

/**
 * Factory to create bootstraps compatible with overlay instance configurations
 * 
 */
public abstract class BootstrapFactory {

	private static final List<BootstrapFactory> bootstrapFactories = new ArrayList<BootstrapFactory>();

	/**
	 * @return true if this factory is compatible with the specified
	 *         configuration
	 */
	public abstract boolean isCompatibleWith(Configuration conf);

	protected abstract Bootstrap createBootstrap(Configuration conf);

	/**
	 * Register a connector factory, only one instance for each factory class
	 * can
	 * be registered
	 * 
	 * @param connectorFactory
	 */
	public static void register(BootstrapFactory connectorFactory) {
		bootstrapFactories.add(connectorFactory);
	}

	/**
	 * Unregister a connector factory implementation
	 * 
	 * @param cls
	 *            the factory class to unregister
	 */
	public static void unregister(BootstrapFactory connectorFactory) {
		bootstrapFactories.remove(connectorFactory);
	}

	/**
	 * Create a new overlay bootstrap initialized with the specified
	 * configuration
	 * 
	 * @throws NoSuchFactoryException
	 * @throws ConfigurationException
	 */
	public static Bootstrap newBootstrap(Configuration c) throws NoSuchFactoryException {
		BootstrapFactory factory = null;

		for (BootstrapFactory f : bootstrapFactories)
			if (f.isCompatibleWith(c))
				factory = f;

		if (factory == null)
			throw new NoSuchFactoryException(c.get(Configuration.OVERLAY_NAME));

		return factory.createBootstrap(c);
	}

	/**
	 * Indicates that no registered factory compatible with the overlay was
	 * found
	 */
	static class NoSuchFactoryException extends Exception {

		NoSuchFactoryException(String instanceName) {
			super("No compatible bootstrap factory for RELOAD overlay instance " + instanceName);
		}
	}
}
