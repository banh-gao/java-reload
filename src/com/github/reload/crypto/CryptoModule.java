package com.github.reload.crypto;

import dagger.Module;
import dagger.Provides;

@Module
public class CryptoModule {

	@Provides
	public Keystore provideKeystore() {
		return new MemoryKeystore();
	}
}
