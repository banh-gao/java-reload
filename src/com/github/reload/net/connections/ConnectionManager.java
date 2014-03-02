package com.github.reload.net.connections;

import java.util.Map;
import com.github.reload.message.RoutableID;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

/**
 * Establish and manage connections for all neighbor nodes
 */
public class ConnectionManager {

	private final Map<RoutableID, ListenableFuture<Connection>> pendingConnections = Maps.newHashMap();

	public ListenableFuture<Connection> connect(RoutableID destination) {
		SettableFuture<Connection> fut = SettableFuture.create();
		// TODO: create connector to manages new connections through attach
		// messages
		return fut;
	}
}
