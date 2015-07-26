package com.github.reload.net;

import javax.inject.Singleton;
import com.github.reload.net.connections.ConnectionManager;
import com.github.reload.net.encoders.MessageBuilder;
import com.github.reload.routing.DefaultPathCompressor;
import com.github.reload.routing.PathCompressor;
import dagger.Module;
import dagger.Provides;

@Module
public class NetModule {

	@Provides
	@Singleton
	MessageBuilder provideMessageBuilder() {
		return new MessageBuilder();
	}

	@Provides
	@Singleton
	MessageRouter provideMessageRounter() {
		return new MessageRouter();
	}

	@Provides
	@Singleton
	ConnectionManager provideConnectionManager() {
		return new ConnectionManager();
	}

	@Provides
	@Singleton
	PathCompressor providePathCompressor() {
		return new DefaultPathCompressor();
	}
}
