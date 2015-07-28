package com.github.reload.services.storage;

import javax.inject.Named;
import javax.inject.Singleton;
import com.github.reload.crypto.Keystore;
import com.github.reload.routing.TopologyPlugin;
import com.github.reload.services.storage.policies.NodeMatch;
import com.github.reload.services.storage.policies.NodeMatch.NodeParamsGenerator;
import com.github.reload.services.storage.policies.UserMatch;
import com.github.reload.services.storage.policies.UserMatch.UserParamsGenerator;
import dagger.Module;
import dagger.Provides;

@Module(injects = {StorageService.class, StorageController.class,
					PreparedData.class, UserParamsGenerator.class,
					NodeParamsGenerator.class, NodeMatch.class,
					UserMatch.class, NodeMatch.NodeParamsGenerator.class,
					UserMatch.UserParamsGenerator.class}, complete = false)
public class StorageModule {

	@Provides
	@Singleton
	StorageService provideStorageService() {
		return new StorageService();
	}

	@Provides
	@Singleton
	StorageController provideStorageController() {
		return new StorageController();
	}

	@Provides
	@Named("node-match")
	AccessPolicy provideNodeMatch(TopologyPlugin topology, Keystore keystore) {
		return new NodeMatch(topology, keystore);
	}

	@Provides
	@Named("user-match")
	AccessPolicy provideUserMatch(TopologyPlugin topology, Keystore keystore) {
		return new UserMatch(topology, keystore);
	}
}
