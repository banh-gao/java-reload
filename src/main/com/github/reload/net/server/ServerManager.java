package com.github.reload.net.server;

import com.github.reload.Context;
import com.github.reload.Context.Component;
import com.github.reload.Context.CtxComponent;
import com.github.reload.ReloadConnector;

public class ServerManager implements Component {

	@CtxComponent
	private ReloadConnector connector;

	private ReloadServer attachServer;
	private ReloadServer bootServer;
	private DiscoveryResponder discoveryResp;

	@Override
	public void compStart(Context context) {
		startAttachServer(context);
		startBootstrapServer(context);
	}

	private void startAttachServer(Context context) {
		attachServer = new ReloadServer(context, connector.getAttachAddress());
		attachServer.startAsync();
	}

	private void startBootstrapServer(Context context) {
		if (!connector.isBootstrapNode())
			return;
		bootServer = new ReloadServer(context, connector.getBootstrapAddress());
		bootServer.startAsync();

		if (connector.getBootstrapAddress().getAddress().isMulticastAddress())
			startDiscoveryResponder(context);
	}

	private void startDiscoveryResponder(Context context) {
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
