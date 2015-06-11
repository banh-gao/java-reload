package com.github.reload;

import com.github.reload.components.ComponentsContext;
import com.github.reload.components.ComponentsContext.Service;
import com.github.reload.components.ComponentsContext.ServiceIdentifier;
import com.github.reload.components.ComponentsRepository.Component;

/**
 * Helper class for app attach handling
 * 
 */
@Component(TestService.class)
public class TestService {

	public static final ServiceIdentifier<TestService> SERVICE_ID = new ServiceIdentifier<TestService>(TestService.class);

	@Component
	private ComponentsContext ctx;

	public ComponentsContext getCtx() {
		return ctx;
	}

	@Service
	private TestService exportService() {
		return this;
	}
}
