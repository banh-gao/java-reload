package com.github.reload;

import java.net.InetSocketAddress;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.apache.log4j.PropertyConfigurator;
import com.github.reload.conf.Configuration;
import com.github.reload.net.ConnectionManager;
import com.github.reload.net.codecs.header.NodeID;
import com.github.reload.routing.TopologyPlugin;
import com.google.common.eventbus.EventBus;
import com.google.common.util.concurrent.ListenableFuture;
import dagger.ObjectGraph;

/**
 * Represents the RELOAD overlay where the local node is connected to
 * 
 */
@Singleton
public class Overlay {

	public static final String LIB_COMPANY = "zeroDenial";
	public static final String LIB_VERSION = "java-reload/0.1";

	public static final byte RELOAD_PROTOCOL_VERSION = 0x0a;

	static {
		PropertyConfigurator.configure("log4j.properties");
	}

	private Configuration conf;
	private InetSocketAddress localAddress;
	private boolean isOverlayInitiator;
	private boolean isClientMode;
	private NodeID localNodeId;
	private byte[] joinData;

	private OverlayConnector connector;
	private ServiceLoader serviceLoader;

	private TopologyPlugin topology;

	ObjectGraph graph;

	@Inject
	public Overlay(ConnectionManager connMgr, TopologyPlugin topology) {
		connector = new OverlayConnector(this, topology, connMgr);
		this.topology = topology;
	}

	void init(Bootstrap bootstrap, CoreModule coreModule) {
		graph = coreModule.graph;
		conf = bootstrap.getConfiguration();
		localAddress = bootstrap.getLocalAddress();
		isOverlayInitiator = bootstrap.isOverlayInitiator();
		isClientMode = bootstrap.isClientMode();
		localNodeId = bootstrap.getLocalNodeId();
		joinData = bootstrap.getJoinData();

		coreModule.graph.inject(topology);

		serviceLoader = new ServiceLoader(coreModule);
	}

	ListenableFuture<Overlay> connect() {
		return connector.connectToOverlay(conf);
	}

	public <T> T getService(Class<T> service) {
		return serviceLoader.getService(service);
	}

	public InetSocketAddress getLocalAddress() {
		return localAddress;
	}

	public boolean isOverlayInitiator() {
		return isOverlayInitiator;
	}

	public boolean isClientMode() {
		return isClientMode;
	}

	public NodeID getLocalNodeId() {
		return localNodeId;
	}

	public Configuration getConfiguration() {
		return conf;
	}

	/**
	 * Disconnect from this overlay and release all the resources. This method
	 * returns when the overlay has been left. All subsequent requests to this
	 * instance will fail.
	 */
	public void disconnect() {
		EventBus eventBus = graph.get(EventBus.class);
		eventBus.post(GlobalEvent.SHUTDOWN);
	}

	@Override
	public String toString() {
		return "OverlayConnection [overlay=" + conf.get(Configuration.OVERLAY_NAME) + ", localId=" + getLocalNodeId() + "]";
	}

	public byte[] getJoinData() {
		return joinData;
	}

	public static class GlobalEvent {

		public static final GlobalEvent SHUTDOWN = new GlobalEvent(TYPE.SHUTDOWN);

		public static enum TYPE {
			SHUTDOWN
		};

		public final TYPE type;

		private GlobalEvent(TYPE type) {
			this.type = type;
		}
	}
}