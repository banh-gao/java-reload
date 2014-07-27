package com.github.reload;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import com.github.reload.Components.Component;
import com.github.reload.conf.Configuration;
import com.github.reload.net.encoders.Message;
import com.github.reload.net.encoders.content.errors.NetworkException;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

/**
 * Represents the RELOAD overlay where the local node is connected to
 * 
 */
@Component(Overlay.COMPNAME)
public class Overlay {

	public static final String COMPNAME = "com.github.reload.Overlay";

	public static final String LIB_COMPANY = "zeroDenial";
	public static final String LIB_VERSION = "java-reload/0.1";

	public static final byte RELOAD_PROTOCOL_VERSION = 0x0a;

	private static final Logger logger = Logger.getLogger(Overlay.class);

	static {
		PropertyConfigurator.configure("log4j.properties");
	}

	@Component
	private Configuration conf;

	@Component
	private Bootstrap bootstrap;

	/**
	 * Joins to the overlay
	 * 
	 * @throws NetworkException
	 * @throws OverlayJoinException
	 * @throws InterruptedException
	 */
	public ListenableFuture<Void> join() {

		final SettableFuture<Void> joinFut = SettableFuture.create();

		ListenableFuture<Message> joinAnsFut = OverlayConnector.join();

		logger.info("Joining to RELOAD overlay " + conf.getOverlayName() + " with " + bootstrap.getLocalNodeId() + " in progress...");

		Futures.addCallback(joinAnsFut, new FutureCallback<Message>() {

			public void onSuccess(Message joinAns) {
				// TODO: check join answer with topology plugin
				joinFut.set(null);
				logger.info("Joining to RELOAD overlay " + conf.getOverlayName() + " with " + bootstrap.getLocalNodeId() + " completed.");
			};

			@Override
			public void onFailure(Throwable t) {
				joinFut.setException(t);
			}
		});

		return joinFut;
	}

	/**
	 * Leave this overlay and release all the resources. This method returns
	 * when the overlay has been left. All subsequent requests to this
	 * instance will fail.
	 */
	public void leave() {
		Components.deinitComponents();
	}

	@Override
	public int hashCode() {
		return bootstrap.hashCode();
	}

	/**
	 * Two overlay instances are considered equals if the assigned connectors
	 * are
	 * equals
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Overlay other = (Overlay) obj;
		return bootstrap.equals(other.bootstrap);
	}

	@Override
	public String toString() {
		return "OverlayConnection [overlay=" + conf.getOverlayName() + ", localId=" + bootstrap.getLocalNodeId() + "]";
	}
}