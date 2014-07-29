package com.github.reload;

import java.util.ArrayList;
import java.util.List;
import javax.naming.ConfigurationException;
import com.github.reload.conf.Configuration;
import com.github.reload.util.ConfigurationFetcher;

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

	protected abstract Bootstrap implCreateBootstrap(Configuration conf);

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
	 * Create a new overlay bootstrap for the specified overlay. The
	 * configuration is automatically fetched from the overlay server.
	 * 
	 * @throws ConfigurationException
	 * @throws NoSuchFactoryException
	 */
	public static Bootstrap createBootstrap(String instanceName) throws ConfigurationException, NoSuchFactoryException {
		Configuration conf = ConfigurationFetcher.fetchConfiguration(instanceName);
		return createBootstrap(conf);
	}

	/**
	 * Create a new overlay bootstrap initialized with the specified
	 * configuration
	 * 
	 * @throws NoSuchFactoryException
	 * @throws ConfigurationException
	 */
	public static Bootstrap createBootstrap(Configuration conf) throws NoSuchFactoryException {
		BootstrapFactory f = getInstance(conf);
		return f.implCreateBootstrap(conf);
	}

	private static BootstrapFactory getInstance(Configuration conf) throws NoSuchFactoryException {
		for (BootstrapFactory f : bootstrapFactories) {
			if (f.isCompatibleWith(conf))
				return f;
		}
		throw new NoSuchFactoryException(conf.getOverlayName());
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
