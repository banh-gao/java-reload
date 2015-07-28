package com.github.reload.crypto;

import javax.inject.Singleton;
import com.github.reload.Bootstrap;
import dagger.Module;
import dagger.Provides;

@Module(injects = {Keystore.class, CryptoHelper.class}, complete = false)
public class CryptoModule {

	private final Keystore keystore;
	private final CryptoHelper cryptoHelper;

	public CryptoModule(Bootstrap boot) {
		keystore = new MemoryKeystore(boot.getLocalCert(), boot.getLocalKey());
		cryptoHelper = new X509CryptoHelper(boot.getSignHashAlg(), boot.getSignAlg(), boot.getHashAlg());
	}

	@Provides
	@Singleton
	public Keystore provideKeystore() {
		return keystore;
	}

	@Provides
	@Singleton
	public CryptoHelper provideCryptoHelper() {
		return cryptoHelper;
	}
}
