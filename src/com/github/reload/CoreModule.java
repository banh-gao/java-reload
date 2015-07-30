package com.github.reload;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import javax.inject.Named;
import javax.inject.Singleton;
import com.github.reload.components.MessageHandlersManager;
import com.github.reload.conf.Configuration;
import com.github.reload.crypto.CryptoHelper;
import com.github.reload.crypto.Keystore;
import com.github.reload.net.MessageRouter;
import com.github.reload.net.connections.ConnectionManager;
import com.github.reload.net.encoders.MessageBuilder;
import com.github.reload.net.ice.ICEHelper;
import com.github.reload.routing.DefaultPathCompressor;
import com.github.reload.routing.PathCompressor;
import com.github.reload.routing.TopologyPlugin;
import dagger.Module;
import dagger.ObjectGraph;
import dagger.Provides;

@Module(injects = {Overlay.class, ObjectGraph.class, MessageBuilder.class,
					ConnectionManager.class, MessageRouter.class,
					DefaultPathCompressor.class, ICEHelper.class,
					MessageHandlersManager.class}, complete = false)
public class CoreModule {

	ObjectGraph graph;

	public void loadModules(Class<?>... modules) {
		for (Class<?> mod : modules) {
			try {
				graph = graph.plus(mod.newInstance());
			} catch (InstantiationException | IllegalAccessException e) {
				throw new IllegalArgumentException(e);
			}
		}
	}

	@Provides
	@Singleton
	Overlay provideOverlay(ConnectionManager connMgr, TopologyPlugin topology) {
		return new Overlay(connMgr, topology);
	}

	@Provides
	ObjectGraph provideGraph() {
		return graph;
	}

	@Provides
	@Singleton
	MessageBuilder provideMessageBuilder(Overlay overlay, Configuration conf) {
		return new MessageBuilder(overlay, conf);
	}

	@Provides
	@Singleton
	MessageHandlersManager provideMessageHandlersManager() {
		return new MessageHandlersManager();
	}

	@Provides
	@Singleton
	public ConnectionManager provideConnectionManager(CryptoHelper cryptoHelper, Keystore keystore) {
		return new ConnectionManager(cryptoHelper, keystore);
	}

	@Provides
	@Singleton
	@Named("packetsLooper")
	Executor provideLoopExecutor() {
		return Executors.newSingleThreadExecutor();
	}

	@Provides
	@Singleton
	MessageRouter provideMessageRounter(ConnectionManager connManager, MessageBuilder msgBuilder, TopologyPlugin topology, @Named("packetsLooper") Executor exec) {
		return new MessageRouter(connManager, msgBuilder, topology, exec);
	}

	@Provides
	@Singleton
	PathCompressor providePathCompressor() {
		return new DefaultPathCompressor();
	}

	@Provides
	@Singleton
	ICEHelper provideIceHelper() {
		return new ICEHelper();
	}
}
