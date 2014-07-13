package com.github.reload;

import java.security.SignatureException;
import java.util.ArrayList;
import java.util.List;
import javax.naming.ConfigurationException;
import com.github.reload.util.ConfigurationFetcher;

/**
 * Factory to create connectors compatible with overlay instance configurations
 * 
 */
public abstract class ReloadConnectorFactory {

	private static final List<ReloadConnectorFactory> connectorFactories = new ArrayList<ReloadConnectorFactory>();

	/**
	 * @return true if this factory is compatible with the specified
	 *         configuration
	 */
	public abstract boolean isCompatibleWith(Configuration conf);

	/**
	 * Create a new connector initialized for the specified reload overlay
	 * 
	 * @throws SignatureException
	 *             if the signature over the configuration is not valid
	 * @throws ConfigurationException
	 */
	public final ReloadConnector createConnector(String instanceName) throws SignatureException, ConfigurationException {
		Configuration conf = ConfigurationFetcher.fetchConfiguration(instanceName);
		return createConnector(conf, null);
	}

	/**
	 * Create a new connector initialized with the specified configuration, uses
	 * the given connector to further initialization
	 * 
	 * @throws SignatureException
	 *             if the signature over the configuration is not valid
	 */
	abstract ReloadConnector createConnector(Configuration conf, ReloadConnector connector) throws SignatureException;

	/**
	 * Register a connector factory, only one instance for each factory class
	 * can
	 * be registered
	 * 
	 * @param connectorFactory
	 */
	public static void register(ReloadConnectorFactory connectorFactory) {
		connectorFactories.add(connectorFactory);
	}

	/**
	 * Unregister a connector factory implementation
	 * 
	 * @param cls
	 *            the factory class to unregister
	 */
	public static void unregister(ReloadConnectorFactory connectorFactory) {
		connectorFactories.remove(connectorFactory);
	}

	/**
	 * Get a connector factory instance compatible with the specified
	 * overlay. It
	 * tries to fetch the overlay configuration at the network location derived
	 * from the instance name.
	 * 
	 * @throws NoSuchFactoryException
	 *             if no registered factory is compatible with the specified
	 *             overlay configuration
	 * @throws ConfigurationException
	 */
	public static ReloadConnectorFactory getInstance(String instanceName) throws NoSuchFactoryException, ConfigurationException {
		Configuration conf = ConfigurationFetcher.fetchConfiguration(instanceName);
		for (ReloadConnectorFactory f : connectorFactories) {
			if (f.isCompatibleWith(conf))
				return f;
		}
		throw new NoSuchFactoryException(instanceName);
	}

	/**
	 * Indicates that no registered factory compatible with the overlay was
	 * found
	 * 
	 */
	public static class NoSuchFactoryException extends Exception {

		public NoSuchFactoryException(String instanceName) {
			super("No compatible connector factory for RELOAD overlay instance " + instanceName);
		}
	}
}
