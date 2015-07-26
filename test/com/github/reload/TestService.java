package com.github.reload;

import javax.inject.Inject;
import com.github.reload.components.ComponentsContext;
import com.github.reload.components.ComponentsContext.Service;
import com.github.reload.components.ComponentsContext.ServiceIdentifier;

/**
 * Helper class for app attach handling
 * 
 */
public class TestService {

	public static final ServiceIdentifier<TestService> SERVICE_ID = new ServiceIdentifier<TestService>(TestService.class);

	@Inject
	ComponentsContext ctx;

	public ComponentsContext getCtx() {
		return ctx;
	}

	@Service
	private TestService exportService() {
		return this;
	}
}
