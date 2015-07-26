package com.github.reload;

import com.github.reload.conf.Configuration;
import com.github.reload.services.storage.DataStorage;
import com.github.reload.services.storage.MemoryStorage;
import dagger.Module;
import dagger.Provides;

@Module
public class CoreModule {

	private final Configuration conf;

	public CoreModule(Configuration conf) {
		this.conf = conf;
	}

	@Provides
	Overlay provideOverlay() {
		return new Overlay();
	}

	@Provides
	Configuration provideConfiguration() {
		return conf;
	}

	@Provides
	DataStorage provideDataStorage() {
		return new MemoryStorage();
	}
}
