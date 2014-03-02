package com.github.reload;

import com.github.reload.message.ContentType;
import com.github.reload.net.MessageReceiver.MessageProcessor;
import com.github.reload.net.MessageTransmitter;
import com.github.reload.routing.TopologyPlugin;
import com.github.reload.storage.StorageController;

/**
 * Application context
 */
public class Context {

	public Configuration getConfiguration() {
		// TODO Auto-generated method stub
		return null;
	}

	public StorageController getStorageController() {
		// TODO Auto-generated method stub
		return null;
	}

	public MessageTransmitter getMessageTransmitter() {
		// TODO Auto-generated method stub
		return null;
	}

	public MessageProcessor getMessageProcessor(ContentType cntType) {
		// TODO Auto-generated method stub
		return null;
	}

	public TopologyPlugin getTopologyPlugin() {
		// TODO Auto-generated method stub
		return null;
	}
}
