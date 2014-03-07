package com.github.reload;

import java.io.IOException;
import java.net.Socket;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.net.SocketFactory;
import javax.tools.Diagnostic;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.Priority;
import org.apache.log4j.PropertyConfigurator;
import com.github.reload.crypto.ReloadCertificate;
import com.github.reload.message.DestinationList;
import com.github.reload.message.NodeID;
import com.github.reload.message.ResourceID;
import com.github.reload.message.errors.NetworkException;
import com.github.reload.routing.TopologyPlugin;
import com.github.reload.storage.PreparedData;
import com.github.reload.storage.StorageClientHelper;
import com.github.reload.storage.data.StoredDataSpecifier;
import com.github.reload.storage.errors.UnknownKindException;
import com.github.reload.storage.net.FetchKindResponse;
import com.github.reload.storage.net.StoreKindResponse;

/**
 * Represent the overlay where the local node is connected to, all operations in
 * the overlay passed through this object
 * 
 */
public class ReloadOverlay {

	static final String LIB_COMPANY = "zdenial";
	static final String LIB_VERSION = "jReload/0.2";

	static final byte RELOAD_PROTOCOL_VERSION = 0x0a;

	public static final String[] SUPPORTED_EXTENSIONS = new String[]{"urn:ietf:params:xml:ns:p2p:diagnostics"};
	private static final Logger logger = Logger.getLogger(ReloadOverlay.class);

	private static final Map<ReloadConnector, ReloadOverlay> INSTANCES = new ConcurrentHashMap<ReloadConnector, ReloadOverlay>();

	static {
		PropertyConfigurator.configure("log4j.properties");
	}

	private Context context;
	private final PeerInitializer connStatusHelper;
	private final StorageClientHelper storageHelper;

	/**
	 * Get an overlay connection instance using the given overlay connector,
	 * multiple calls with the same connector returns always the same overlay
	 * instance.
	 * 
	 * @param connector
	 * @return an overlay instance connected to the RELOAD overlay
	 * @throws InitializationException
	 * @throws NetworkException
	 */
	public static ReloadOverlay getInstance(ReloadConnector connector) throws InitializationException, NetworkException {
		ReloadOverlay instance = INSTANCES.get(connector);
		if (instance == null) {
			instance = new ReloadOverlay(connector);
			INSTANCES.put(connector, instance);
		}
		return instance;
	}

	private ReloadOverlay(ReloadConnector connector) throws InitializationException, NetworkException {
		context = new Context();
		context.init(connector);

		connStatusHelper = new PeerInitializer();
		storageHelper = new StorageClientHelper();

	}

	public void connect() throws NetworkException, InitializationException {
		logger.log(Priority.INFO, "Starting RELOAD peer " + context.getLocalId() + " for overlay " + context.getConfiguration().getOverlayName() + "...");
		connStatusHelper.startConnections();
		logger.log(Priority.INFO, "RELOAD peer " + context.getLocalId() + " successfully started.");
	}

	/**
	 * Joins to the overlay
	 * 
	 * @throws NetworkException
	 * @throws OverlayJoinException
	 * @throws InterruptedException
	 */
	public void join() throws NetworkException, OverlayJoinException, InterruptedException {
		connStatusHelper.checkConnection();

		Configuration conf = context.getConfiguration();
		logger.log(Level.INFO, "Joining to RELOAD overlay " + conf.getOverlayName() + " with " + getLocalId() + " in progress...");
		connStatusHelper.join();
		logger.log(Level.INFO, "Joining to RELOAD overlay " + conf.getOverlayName() + " with " + getLocalId() + " completed.");
	}

	/**
	 * Accessor method to allow Context access to AccessPolicyParamsGenerator
	 * 
	 * @return the overlay context for this connection
	 */
	Context getContext() {
		return context;
	}

	/**
	 * @return the configuration of this overlay
	 */
	public Configuration getConfiguration() {
		return context.getComponent(Configuration.class);
	}

	/**
	 * Tries to reconnect the connection to the overlay up to
	 * {@value #RECONNECT_ATTEMPTS} times, if reconnect fails all subsequent
	 * calls to
	 * overlay operations will fail with a NetworkException
	 * 
	 * @param reason
	 *            The reason for the reconnecting that will be reported in logs,
	 *            may be null
	 * @return true if the reconnect succeeds, false otherwise
	 */
	public void reconnect(String reason) {
		connStatusHelper.reconnect(reason);
	}

	/**
	 * Leave this overlay and release all the resources. This method returns
	 * when the overlay has been left. All subsequent requests to this
	 * instance will fail.
	 */
	public void leave() {
		connStatusHelper.leave();
		INSTANCES.remove(this);
	}

	/**
	 * @return the data kind associated to the given kind-id
	 * @throws UnknownKindException
	 *             if the given kind-id is not associated to any existing kind
	 */
	public DataKind getDataKind(KindId kindId) throws UnknownKindException {
		return getConfiguration().getDataKind(kindId);
	}

	/**
	 * @return the available data kinds ids
	 */
	public Set<KindId> getDataKindIds() {
		return getConfiguration().getDataKindIds();
	}

	/**
	 * Get status informations about the current overlay instance
	 */
	public PeerStatus getPeerStatus() {
		return context.getPeerStatus();
	}

	/**
	 * @return A diagnostic object that provides an interface to run a set of
	 *         diagnostic tools on the overlay
	 */
	public Diagnostic getDiagnostic() {
		return context.getDiagnostic();
	}

	/**
	 * @return The RELOAD certificate of the local peer
	 */
	public ReloadCertificate getLocalCertificate() {
		return context.getLocalCert();
	}

	/**
	 * @return The node-id of the local peer used as to interact with the
	 *         overlay
	 */
	public NodeID getLocalId() {
		return context.getLocalId();
	}

	/**
	 * @return True if this node is in client mode
	 */
	public boolean isClient() {
		return context.isClientMode();
	}

	/**
	 * Set the name of the application that uses the library, the
	 * recommended format is AppName/version
	 * 
	 * @param applicationName
	 */
	public void setApplicationName(String applicationName) {
		context.setApplicationName(applicationName);
	}

	public String getApplicationName() {
		return context.getApplicationName();
	}

	/**
	 * Store specified values into the overlay, warn about the resource-id and
	 * the sender-id that may be restricted by some data-kind access control
	 * policy. This is a blocking call, the method returns only when the
	 * response is received or an exception is throwed.
	 * 
	 * If at the moment of this method call a connection reconnecting is
	 * running,
	 * the caller thread will be locked until the connection is established, if
	 * the connection cannot be established a NetworkException will be throwed.
	 * 
	 * @param destination
	 *            The destination list to the resource-id to store
	 * @param preparedData
	 *            The {@link PreparedData} to be stored, can be of different
	 *            data-kinds
	 * 
	 * @throws StorageException
	 *             if the storer node reports an error in store procedure
	 * @throws NetworkException
	 *             if a network error occurs while storing the data
	 * @throws InterruptedException
	 *             if the caller thread is interrupted while waiting for the
	 *             response
	 * @throws NullPointerException
	 *             if some argument is null
	 * @throws IllegalArgumentException
	 *             if the resource id length exceeds from the length used by the
	 *             overlay algorithm or if the final id in the destination list
	 *             is not a resource
	 */
	public List<StoreKindResponse> storeData(DestinationList destination, PreparedData... preparedData) {
		if (destination == null || preparedData == null)
			throw new NullPointerException();

		if (!destination.isResourceDestination())
			throw new IllegalArgumentException("The destination must point to a resource-id");

		connStatusHelper.checkConnection();

		return storageHelper.sendStoreRequest(destination, preparedData);
	}

	/**
	 * Store specified values into the overlay, warn about the resource-id and
	 * the sender-id that may be restricted by some data-kind access control
	 * policy. This is a blocking call, the method returns only when the
	 * response is received or an exception is throwed.
	 * 
	 * @param destination
	 *            The destination list to the resource-id to store
	 * @param preparedData
	 *            The {@link PreparedData} to be stored, can be of different
	 *            data-kinds
	 * 
	 * @throws NullPointerException
	 *             if some argument is null
	 * @throws IllegalArgumentException
	 *             if the resource id length exceeds from the length used by the
	 *             overlay algorithm or if the final id in the destination list
	 *             is not a resource
	 * @throws StorageException
	 *             if the storer node reports an error in store procedure
	 * @throws NetworkException
	 *             if a network error occurs while storing the data
	 * @throws InterruptedException
	 *             if the caller thread is interrupted while waiting for the
	 *             response
	 */
	public List<StoreKindResponse> storeData(DestinationList destination, Collection<? extends PreparedData> preparedData) {
		return storeData(destination, preparedData.toArray(new PreparedData[0]));
	}

	/**
	 * Retrieve the values corresponding to the specified resource-id that
	 * matches the passed data specifiers. This is a blocking call, the method
	 * returns only when the response is received or an exception is throwed.
	 * 
	 * @param destination
	 *            The destination list to the resource-id to fetch
	 * @param specifiers
	 *            The {@link DataSpecifier} to be used to specify what to fetch
	 * 
	 * @throws NullPointerException
	 *             if some argument is null
	 * @throws IllegalArgumentException
	 *             if the resource id length exceeds from the length used by the
	 *             overlay algorithm or if the final id in the destination list
	 *             is not a resource
	 * @throws StorageException
	 *             if the storer node reports an error in the fetch procedure or
	 *             if the fetched data authentication fails
	 * @throws NetworkException
	 *             if a network error occurs while fetching the data
	 * @throws InterruptedException
	 *             if the caller thread is interrupted while waiting for the
	 *             response
	 */
	public List<FetchKindResponse> fetchData(DestinationList destination, StoredDataSpecifier... specifiers) {
		if (destination == null || specifiers == null)
			throw new NullPointerException();

		if (!destination.isResourceDestination())
			throw new IllegalArgumentException("The destination must point to a resource-id");

		ResourceID resourceId = destination.getResourceDestination();

		connStatusHelper.checkConnection();

		if (resourceId.getData().length > context.getComponent(TopologyPlugin.class).getResourceIdLength())
			throw new IllegalArgumentException("The resource-id exceeds the overlay allowed length of " + context.getTopologyPlugin().getResourceIdLength() + " bytes");

		return storageHelper.sendFetchRequest(destination, specifiers);
	}

	/**
	 * Retrieve the values corresponding to the specified resource-id that
	 * matches the passed data specifiers. This is a blocking call, the method
	 * returns only when the response is received or an exception is throwed.
	 * 
	 * @param destination
	 *            The destination list to the resource-id to fetch
	 * @param specifiers
	 *            The {@link DataSpecifier} to be used to specify what to fetch
	 * 
	 * @throws NullPointerException
	 *             if some argument is null
	 * @throws IllegalArgumentException
	 *             if the resource id length exceeds from the length used by the
	 *             overlay algorithm or if the final id in the destination list
	 *             is not a resource
	 * @throws StorageException
	 *             if the storer node reports an error in the fetch procedure or
	 *             if the fetched data authentication fails
	 * @throws NetworkException
	 *             if a network error occurs while fetching the data
	 * @throws InterruptedException
	 *             if the caller thread is interrupted while waiting for the
	 *             response
	 */
	public List<FetchKindResponse> fetchData(DestinationList destination, Collection<? extends DataSpecifier> specifiers) throws StorageException, NetworkException, InterruptedException {
		return fetchData(destination, specifiers.toArray(new DataSpecifier[0]));
	}

	/**
	 * Remove the data from the overlay by set the exist flag to false, note
	 * that the protocol doesn't define an explicit remove operation, the
	 * request is a store request generated from the data specifier
	 * 
	 * @param destination
	 *            The destination list to the resource-id to remove
	 * @param dataSpecifier
	 *            The specifier to select the data to be removed
	 * @return The responses to the remove operation in form of store responses
	 * @throws NullPointerException
	 *             if some argument is null
	 * @throws IllegalArgumentException
	 *             if the resource id length exceeds from the length used by the
	 *             overlay algorithm or if the final id in the destination list
	 *             is not a resource
	 * @throws StorageException
	 *             if the storer node reports an error in the remove procedure
	 * @throws NetworkException
	 *             if a network error occurs while fetching the data
	 * @throws InterruptedException
	 *             if the caller thread is interrupted while waiting for the
	 *             response
	 */
	public List<StoreResponse> removeData(DestinationList destination, DataSpecifier dataSpecifier) throws StorageException, NetworkException, InterruptedException {
		if (destination == null || dataSpecifier == null)
			throw new NullPointerException();

		if (!destination.isResourceDestination())
			throw new IllegalArgumentException("The destination must point to a resource-id");

		ResourceID resourceId = destination.getResourceDestination();

		connStatusHelper.checkConnection();

		if (resourceId.getData().length > context.getComponent(TopologyPlugin.class).getResourceIdLength())
			throw new IllegalArgumentException("The resource-id exceeds the overlay allowed length of " + context.getTopologyPlugin().getResourceIdLength() + " bytes");

		return storageHelper.sendRemoveRequest(destination, dataSpecifier);
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
	 * @throws ErrorMessageException
	 *             if the destination node response with an error message
	 * @throws IOException
	 *             if some error occurs while connecting to the node
	 * @throws InterruptedException
	 *             if the caller thread is interrupted while waiting for the
	 *             response
	 */
	public Socket connectTo(DestinationList destination, ApplicationID applicationId) throws ErrorMessageException, IOException, InterruptedException {
		connStatusHelper.checkConnection();
		SocketFactory factory = SocketFactory.getDefault();
		return context.getApplicativeManager().sendAppAttach(destination, applicationId, factory);
	}

	/**
	 * Create an application level connection to the node responsible for the
	 * specified destination using the RELOAD appAttach message. The returned
	 * socket is created using the specified socket factory.
	 * 
	 * @param destination
	 *            The destination list to be used to reach the destination node
	 * @param applicationId
	 *            The {@link ApplicationID} of the application that will use
	 *            this connection
	 * @param factory
	 *            The socket factory to be used to create the socket
	 * 
	 * @return A socket used to communicate with the connected node
	 * @throws ErrorMessageException
	 *             if the destination node response with an error message
	 * @throws IOException
	 *             if some error occurs while connecting to the node
	 * @throws InterruptedException
	 *             if the caller thread is interrupted while waiting for the
	 *             response
	 */
	public Socket connectTo(DestinationList destination, final ApplicationID applicationId, SocketFactory factory) throws ErrorMessageException, IOException, InterruptedException {
		connStatusHelper.checkConnection();
		return context.getApplicativeManager().sendAppAttach(destination, applicationId, factory);
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
		return context.getApplicativeManager().registerApplicativeServer(applicationID);
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
		return context.getApplicativeManager().unregisterApplicativeServer(applicationID);
	}

	/**
	 * @return The application servers registered for the local peer
	 */
	public Set<ApplicationID> getRegisteredServers() {
		return context.getApplicativeManager().getRegisteredServers();
	}

	@Override
	public int hashCode() {
		return context.getConnector().hashCode();
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
		ReloadOverlay other = (ReloadOverlay) obj;
		return context.getConnector().equals(other.context.getConnector());
	}

	@Override
	public String toString() {
		if (connStatusHelper.isConnected())
			return "OverlayConnection [overlay=" + context.getComponent(Configuration.class).getOverlayName() + ", localId=" + context.getLocalId() + "]";
		return "OverlayConnection [DISCONNECTED]";
	}
}