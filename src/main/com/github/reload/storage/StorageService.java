package com.github.reload.storage;

import java.security.GeneralSecurityException;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.github.reload.components.ComponentsContext.Service;
import com.github.reload.components.ComponentsContext.ServiceIdentifier;
import com.github.reload.components.ComponentsRepository.Component;
import com.github.reload.conf.Configuration;
import com.github.reload.net.MessageRouter;
import com.github.reload.net.NetworkException;
import com.github.reload.net.encoders.Message;
import com.github.reload.net.encoders.MessageBuilder;
import com.github.reload.net.encoders.content.ContentType;
import com.github.reload.net.encoders.content.Error.ErrorMessageException;
import com.github.reload.net.encoders.header.DestinationList;
import com.github.reload.net.encoders.header.ResourceID;
import com.github.reload.net.encoders.secBlock.SignerIdentity.IdentityType;
import com.github.reload.routing.TopologyPlugin;
import com.github.reload.storage.encoders.ArrayModel.ArrayModelSpecifier;
import com.github.reload.storage.encoders.DictionaryModel.DictionaryModelSpecifier;
import com.github.reload.storage.encoders.DictionaryValue.Key;
import com.github.reload.storage.encoders.FetchAnswer;
import com.github.reload.storage.encoders.FetchKindResponse;
import com.github.reload.storage.encoders.FetchRequest;
import com.github.reload.storage.encoders.StoreAnswer;
import com.github.reload.storage.encoders.StoreKindData;
import com.github.reload.storage.encoders.StoreKindResponse;
import com.github.reload.storage.encoders.StoreRequest;
import com.github.reload.storage.encoders.StoredData;
import com.github.reload.storage.encoders.StoredDataSpecifier;

/**
 * Helps a peer to send storage requests into the overlay
 * 
 */
public class StorageService {

	public static final ServiceIdentifier<StorageService> SERVICE_ID = new ServiceIdentifier<StorageService>(StorageService.class);

	private static final short REPLICA_NUMBER = 0;

	@Component
	private Configuration conf;

	@Component
	private TopologyPlugin topologyPlugin;

	@Component
	private MessageRouter msgRouter;

	@Component
	private MessageBuilder msgBuilder;

	@Service
	private StorageService exportService() {
		return this;
	}

	/**
	 * @return the data kind associated to the given kind-id
	 * @throws UnknownKindException
	 *             if the given kind-id is not associated to any existing kind
	 */
	public DataKind getDataKind(Long kindId) throws UnknownKindException {
		return conf.getDataKind(kindId);
	}

	/**
	 * @return the available data kinds
	 */
	public Set<DataKind> getDataKinds() {
		return conf.getDataKinds();
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

		return sendStoreRequest(destination, preparedData);
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

	private List<StoreKindResponse> sendStoreRequest(DestinationList destination, PreparedData... preparedData) {
		ResourceID resourceId;
		try {
			resourceId = (ResourceID) destination.get(destination.size() - 1);
		} catch (Exception e) {
			throw new IllegalArgumentException("Invalid destination");
		}

		if (preparedData.length == 0)
			return Collections.emptyList();

		if (resourceId.getData().length > topologyPlugin.getResourceIdLength())
			throw new IllegalArgumentException("The resource-id exceeds the overlay allowed length of " + context.getTopologyPlugin().getResourceIdLength() + " bytes");

		Map<KindId, StoreKindData> kindData = new HashMap<KindId, StoreKindData>();

		for (PreparedData prepared : preparedData) {
			StoredData data = prepared.build(context, resourceId);
			StoreKindData kd = kindData.get(prepared.getKind().getKindId());
			if (kd == null) {
				kd = new StoreKindData(data.getKind(), prepared.getGeneration());
				kindData.put(data.getKind().getKindId(), kd);
			}
			kd.add(data);
		}

		Message response;

		try {
			Message request = msgBuilder.newMessage(new StoreRequest(resourceId, REPLICA_NUMBER, new ArrayList<StoreKindData>(kindData.values())), destination);

			response = msgRouter.sendRequestMessage(request);
		} catch (ErrorMessageException e) {
			if (!(e instanceof StorageException)) {
				e = new StorageException(e.getError().getStringInfo());
			}
			throw (StorageException) e;
		}

		if (response.getContent().getType() != ContentType.STORE_ANS)
			throw new NetworkException("Invalid store answer");

		StoreAnswer answer = (StoreAnswer) response.getContent();
		return answer.getResponses();
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

	private List<FetchKindResponse> sendFetchRequest(DestinationList destination, StoredDataSpecifier... specifiers) {
		ResourceID resourceId;
		try {
			resourceId = (ResourceID) destination.get(destination.size() - 1);
		} catch (Exception e) {
			throw new IllegalArgumentException("Invalid destination");
		}
		Message response;
		try {
			Message message = msgBuilder.newMessage(new FetchRequest(resourceId, specifiers), destination);
			response = msgRouter.sendRequestMessage(message);
		} catch (ErrorMessageException e) {
			if (!(e instanceof StorageException)) {
				e = new StorageException(e.getError().getStringInfo());
			}
			throw (StorageException) e;
		}

		if (response.getContent().getType() != ContentType.FETCH_ANS)
			throw new NetworkException("Invalid fetch answer");

		FetchAnswer answer = (FetchAnswer) response.getContent();

		for (FetchKindResponse r : answer.getResponses()) {
			for (StoredData data : r.getValues()) {
				try {
					// Synthetic values are not authenticated
					if (data.getValue().exists() || data.getSignature().getIdentity().getIdentityType() != IdentityType.NONE) {
						Certificate signerCert = context.getCryptoHelper().getCertificate(data.getSignature().getIdentity()).getOriginalCertificate();
						data.verify(signerCert, resourceId);
					}
				} catch (GeneralSecurityException e) {
					throw new StorageException("Fetched data authentication failed");
				}
			}
		}
		return answer.getResponses();
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

	private List<StoreKindResponse> sendRemoveRequest(DestinationList destination, StoredDataSpecifier dataSpecifier) throws StorageException, NetworkException, InterruptedException {
		DataKind kind = dataSpecifier.getDataKind();

		StoredDataSpecifier modelSpec = dataSpecifier.getModelSpecifier();

		List<PreparedData> preparedDatas = new ArrayList<PreparedData>();

		if (modelSpec instanceof ArrayModelSpecifier) {
			Set<Long> settedIndexes = new HashSet<Long>();
			for (ArrayRange r : ((ArrayModelSpecifier) modelSpec).getRanges()) {
				for (long i = r.getStartIndex(); i < r.getEndIndex(); i++) {
					PreparedData b = kind.newPreparedData();
					ArrayPreparedValue preparedVal = (ArrayPreparedValue) b.getValue();
					if (settedIndexes.contains(i)) {
						continue;
					}
					preparedVal.setIndex(i);
					settedIndexes.add(i);
					preparedDatas.add(b);
				}
			}
		} else if (modelSpec instanceof DictionaryModelSpecifier) {
			for (Key k : ((DictionaryModelSpecifier) modelSpec).getKeys()) {
				PreparedData b = kind.newPreparedData();
				DictionaryPreparedValue preparedVal = (DictionaryPreparedValue) b.getValue();
				preparedVal.setKey(k);
				preparedDatas.add(b);
			}
		} else {
			preparedDatas.add(kind.newPreparedData());
		}

		for (PreparedData b : preparedDatas) {
			b.setLifeTime(EncUtils.maxUnsignedInt(EncUtils.U_INT32));
			b.getValue().setExists(false);
		}

		return sendStoreRequest(destination, preparedDatas.toArray(new PreparedData[0]));
	}
}
