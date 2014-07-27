package com.github.reload.appAttach;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Set;
import com.github.reload.ApplicationID;
import com.github.reload.net.encoders.header.DestinationList;
import com.google.common.util.concurrent.ListenableFuture;

public class AppAttachService {

	private final AppAttachComponent appAttachComp;

	public AppAttachService(AppAttachComponent appAttachComp) {
		this.appAttachComp = appAttachComp;
	}

	/**
	 * Create an application level connection to the node responsible for the
	 * specified destination using the RELOAD appAttach message. The returned
	 * sockets are generated using the default socket factory.
	 * 
	 * @param destinationList
	 *            The destination list to be used to reach the destination node
	 * @param applicationId
	 *            The {@link ApplicationID} of the application that will use
	 *            this connection
	 * 
	 * @return A socket used to communicate with the connected node
	 * @throws IOException
	 *             if some error occurs while connecting to the node
	 * @throws InterruptedException
	 *             if the caller thread is interrupted while waiting for the
	 *             response
	 */
	public ListenableFuture<InetSocketAddress> connectTo(DestinationList destination, ApplicationID applicationId) throws IOException, InterruptedException {
		connStatusHelper.checkConnection();
		return appAttachComp.sendAppAttach(destination, applicationId);
	}

	/**
	 * Register an application server that is already listening to the specified
	 * application-id port. Only one application-id can be registered, the
	 * registration fails if there is already a registered server for the
	 * related application-id.
	 * 
	 * @param applicationID
	 *            the application identificator of a running application server
	 * 
	 * @return true if the server registration succeeds, false otherwise
	 */
	public boolean registerApplicativeServer(ApplicationID applicationID) {
		return appAttachComp.registerApplicativeServer(applicationID);
	}

	/**
	 * Unregister the application server related to the specified applicationID
	 * 
	 * @param applicationID
	 *            The {@link ApplicationID} of the applicative server to
	 *            unregister
	 * @return true if the server was unregistered, false otherwise
	 */
	public boolean unregisterApplicativeServer(ApplicationID applicationID) {
		return appAttachComp.unregisterApplicativeServer(applicationID);
	}

	/**
	 * @return The application servers registered for the local peer
	 */
	public Set<ApplicationID> getRegisteredServers() {
		return appAttachComp.getRegisteredServers();
	}
}
