package com.github.reload.crypto;

import javax.inject.Singleton;
import com.github.reload.Bootstrap;
import com.github.reload.conf.Configuration;
import com.github.reload.net.encoders.secBlock.HashAlgorithm;
import com.github.reload.net.encoders.secBlock.SignatureAlgorithm;
import dagger.Module;
import dagger.Provides;

@Module(injects = {Keystore.class, CryptoHelper.class}, complete = false)
public class CryptoModule {

	private final Keystore keystore;

	private final HashAlgorithm signHashAlg;
	private final SignatureAlgorithm signAlg;
	private final HashAlgorithm hashAlg;

	public CryptoModule(Bootstrap boot) {
		keystore = new MemoryKeystore(boot.getLocalCert(), boot.getLocalKey());
		signHashAlg = boot.getSignHashAlg();
		signAlg = boot.getSignAlg();
		hashAlg = boot.getHashAlg();
	}

	@Provides
	@Singleton
	public Keystore provideKeystore() {
		return keystore;
	}

	@Provides
	@Singleton
	public CryptoHelper provideCryptoHelper(Keystore keystore, Configuration conf) {
		return new X509CryptoHelper(keystore, conf, signHashAlg, signAlg, hashAlg);
	}
}
