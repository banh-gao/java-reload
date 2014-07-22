package com.github.reload.net.server;

import com.github.reload.Components;
import com.github.reload.Components.Component;
import com.github.reload.Components.CtxComponent;
import com.github.reload.ReloadConnector;

public class ServerManager implements Component {

	@CtxComponent
	private ReloadConnector connector;

	private ReloadServer attachServer;
	private ReloadServer bootServer;
	private DiscoveryResponder discoveryResp;

	@Override
	public void compStart(Components context) {
		startAttachServer(context);
		startBootstrapServer(context);
	}

	private void startAttachServer(Components context) {
		attachServer = new ReloadServer(context, connector.getAttachAddress());
		attachServer.startAsync();
	}

	private void startBootstrapServer(Components context) {
		if (!connector.isBootstrapNode())
			return;
		bootServer = new ReloadServer(context, connector.getBootstrapAddress());
		bootServer.startAsync();

		if (connector.getBootstrapAddress().getAddress().isMulticastAddress())
			startDiscoveryResponder(context);
	}

	private void startDiscoveryResponder(Components context) {
		discoveryResp = new DiscoveryResponder(context, connector.getBootstrapAddress());
		discoveryResp.startAsync();
	}

	public ReloadServer getAttachServer() {
		return attachServer;
	}

	public ReloadServer getBootServer() {
		return bootServer;
	}

}
