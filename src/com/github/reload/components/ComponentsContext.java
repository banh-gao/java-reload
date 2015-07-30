package com.github.reload.components;

import org.apache.log4j.Logger;
import com.google.common.eventbus.DeadEvent;
import com.google.common.eventbus.Subscribe;

public class ComponentsContext {

	public static ComponentsContext newInstance() {
		return new ComponentsContext();
	}

	@Subscribe
	public void handleDeadEvents(DeadEvent ev) {
		Logger.getRootLogger().trace("Ignored event " + ev.getEvent());
	}
}
