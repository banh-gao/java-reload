package com.github.reload;

import java.security.SignatureException;
import java.util.ArrayList;
import java.util.List;
import net.sf.jReload.configuration.Configuration;

/**
 * Factory to create connectors compatible with overlay instance configurations
 * 
 * @author Daniel Zozin <zdenial@gmx.com>
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
	 * Create a new connector initialized with the specified configuration
	 * 
	 * @throws SignatureException
	 *             if the signature over the configuration is not valid
	 */
	public final ReloadConnector createConnector(Configuration conf) throws SignatureException {
		return createConnector(conf, null);
	}

	/**
	 * Create a new connector initialized with the specified configuration, uses
	 * the given connector to further initialization
	 * 
	 * @throws SignatureException
	 *             if the signature over the configuration is not valid
	 */
	public abstract ReloadConnector createConnector(Configuration conf, ReloadConnector connector) throws SignatureException;

	/**
	 * Register a connector factory, only one instance for each factory class can
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
	 * Get a connector factory instance compatible with the specified configuration
	 * 
	 * @throws NoSuchFactoryException
	 *             if no registered factory is compatible with the specified
	 *             overlay configuration
	 */
	public static ReloadConnectorFactory getInstance(Configuration configuration) throws NoSuchFactoryException {
		for (ReloadConnectorFactory f : connectorFactories) {
			if (f.isCompatibleWith(configuration))
				return f;
		}
		throw new NoSuchFactoryException(configuration.getOverlayName());
	}

	/**
	 * Indicates that no registered factory compatible with the overlay was
	 * found
	 * 
	 * @author Daniel Zozin <zdenial@gmx.com>
	 * 
	 */
	public static class NoSuchFactoryException extends Exception {

		public NoSuchFactoryException(String instanceName) {
			super("No compatible connector factory for RELOAD overlay instance " + instanceName);
		}
	}
}
