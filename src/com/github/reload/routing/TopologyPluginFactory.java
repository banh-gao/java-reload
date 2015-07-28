package com.github.reload.routing;

import java.util.HashMap;
import java.util.Map;

public class TopologyPluginFactory {

	private static final Map<String, Class<? extends TopologyPlugin>> plugins = new HashMap<String, Class<? extends TopologyPlugin>>();

	public static void register(String name, Class<? extends TopologyPlugin> plugin) {
		plugins.put(name, plugin);
	}

	public static TopologyPlugin newInstance(String name) {
		Class<? extends TopologyPlugin> clazz = plugins.get(name);
		if (clazz == null)
			throw new RuntimeException("No topology plugin implementation for " + name);

		try {
			return clazz.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}
}
