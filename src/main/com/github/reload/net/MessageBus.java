package com.github.reload.net;

import com.google.common.eventbus.EventBus;

public class MessageBus {

	private final EventBus msgBus;

	public MessageBus() {
		msgBus = new EventBus();
	}

	public boolean equals(Object obj) {
		return msgBus.equals(obj);
	}

	public int hashCode() {
		return msgBus.hashCode();
	}

	public void register(Object object) {
		msgBus.register(object);
	}

	public void post(Object event) {
		msgBus.post(event);
	}

	public String toString() {
		return msgBus.toString();
	}

	public void unregister(Object object) {
		msgBus.unregister(object);
	}

}
