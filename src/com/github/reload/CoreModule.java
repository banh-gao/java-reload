package com.github.reload;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import javax.inject.Named;
import javax.inject.Singleton;
import com.github.reload.net.MessageRouter;
import com.github.reload.net.NetModule;
import com.github.reload.net.codecs.MessageBuilder;
import com.github.reload.net.ice.ICEHelper;
import com.github.reload.routing.DefaultPathCompressor;
import com.github.reload.routing.MessageHandlersManager;
import com.google.common.eventbus.EventBus;
import dagger.Module;
import dagger.ObjectGraph;
import dagger.Provides;

@Module(injects = {Overlay.class, ObjectGraph.class, MessageBuilder.class,
					MessageRouter.class, DefaultPathCompressor.class,
					ICEHelper.class, MessageHandlersManager.class,
					EventBus.class}, includes = {NetModule.class}, complete = false)
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
	ObjectGraph provideGraph() {
		return graph;
	}

	@Provides
	@Singleton
	EventBus provideEventBus() {
		return new EventBus();
	}

	@Provides
	@Singleton
	@Named("packetsLooper")
	Executor provideLoopExecutor() {
		return Executors.newSingleThreadExecutor();
	}
}
