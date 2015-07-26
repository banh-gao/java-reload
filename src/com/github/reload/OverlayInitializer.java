package com.github.reload;

import com.github.reload.conf.Configuration;
import com.github.reload.net.NetModule;
import dagger.Component;

@Component(modules = {CoreModule.class, NetModule.class})
public interface OverlayInitializer {

	Overlay getOverlay();

	Bootstrap getBootstrap();

	Configuration getConfiguration();
}
