package com.github.reload;

import org.apache.log4j.PropertyConfigurator;
import com.github.reload.Components.Component;
import com.github.reload.Components.ServiceIdentifier;
import com.github.reload.conf.Configuration;

/**
 * Represents the RELOAD overlay where the local node is connected to
 * 
 */
@Component(Overlay.COMPNAME)
public class Overlay {

	public static final String COMPNAME = "com.github.reload.Overlay";

	public static final String LIB_COMPANY = "zeroDenial";
	public static final String LIB_VERSION = "java-reload/0.1";

	public static final byte RELOAD_PROTOCOL_VERSION = 0x0a;

	static {
		PropertyConfigurator.configure("log4j.properties");
	}

	@Component
	private Configuration conf;

	@Component
	private Bootstrap bootstrap;

	public <T> T getService(ServiceIdentifier<T> serviceId) {
		return Components.getService(serviceId);
	}

	/**
	 * Leave this overlay and release all the resources. This method returns
	 * when the overlay has been left. All subsequent requests to this
	 * instance will fail.
	 */
	public void leave() {
		Components.deinitComponents();
	}

	@Override
	public int hashCode() {
		return bootstrap.hashCode();
	}

	/**
	 * Two overlay instances are considered equals if the assigned connectors
	 * are
	 * equals
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Overlay other = (Overlay) obj;
		return bootstrap.equals(other.bootstrap);
	}

	@Override
	public String toString() {
		return "OverlayConnection [overlay=" + conf.getOverlayName() + ", localId=" + bootstrap.getLocalNodeId() + "]";
	}
}