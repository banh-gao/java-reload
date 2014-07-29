package com.github.reload;

import com.github.reload.conf.Configuration;

public class TestFactory extends BootstrapFactory {

	@Override
	public boolean isCompatibleWith(Configuration conf) {
		return true;
	}

	@Override
	protected Bootstrap implCreateBootstrap(Configuration conf) {
		try {
			return new TestBootstrap(conf);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

}
