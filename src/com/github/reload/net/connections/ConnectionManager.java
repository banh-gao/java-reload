package com.github.reload.net.connections;

import com.github.reload.message.NodeID;
import com.github.reload.message.RoutableID;
import com.github.reload.net.TransmissionFuture;

/**
 * Establish and manage connections for all neighbor nodes
 */
public class ConnectionManager {

	public ConnectionFuture connect(RoutableID destination) {
		// TODO: create connector to manages new connections through attach
		// messages
	}

	public Connection getConnection(NodeID node) {

	}

	public class ConnectionFuture extends TransmissionFuture {

		public Connection getConnection() {

		}
	}
}
