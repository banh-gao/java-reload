package com.github.reload.net;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import javax.inject.Named;
import javax.inject.Singleton;
import com.github.reload.Overlay;
import com.github.reload.conf.Configuration;
import com.github.reload.crypto.CryptoHelper;
import com.github.reload.crypto.Keystore;
import com.github.reload.net.connections.ConnectionManager;
import com.github.reload.net.encoders.MessageBuilder;
import com.github.reload.net.ice.ICEHelper;
import com.github.reload.routing.DefaultPathCompressor;
import com.github.reload.routing.PathCompressor;
import com.github.reload.routing.TopologyPlugin;
import dagger.Module;
import dagger.Provides;

@Module(library = true, complete = false)
public class NetModule {

	@Provides
	@Singleton
	MessageBuilder provideMessageBuilder(Overlay overlay, Configuration conf) {
		return new MessageBuilder(overlay, conf);
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
