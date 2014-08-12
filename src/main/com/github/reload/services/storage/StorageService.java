package com.github.reload.services.storage;

import java.security.GeneralSecurityException;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.github.reload.components.ComponentsContext;
import com.github.reload.components.ComponentsContext.CompStart;
import com.github.reload.components.ComponentsContext.Service;
import com.github.reload.components.ComponentsContext.ServiceIdentifier;
import com.github.reload.components.ComponentsRepository.Component;
import com.github.reload.conf.Configuration;
import com.github.reload.crypto.CryptoHelper;
import com.github.reload.crypto.ReloadCertificate;
import com.github.reload.net.MessageRouter;
import com.github.reload.net.NetworkException;
import com.github.reload.net.encoders.Message;
import com.github.reload.net.encoders.MessageBuilder;
import com.github.reload.net.encoders.header.DestinationList;
import com.github.reload.net.encoders.header.ResourceID;
import com.github.reload.net.encoders.secBlock.SignerIdentity.IdentityType;
import com.github.reload.routing.TopologyPlugin;
import com.github.reload.services.storage.AccessPolicy.AccessParamsGenerator;
import com.github.reload.services.storage.encoders.ArrayModel.ArrayValueBuilder;
import com.github.reload.services.storage.encoders.ArrayModel.ArrayValueSpecifier;
import com.github.reload.services.storage.encoders.ArrayModel.ArrayValueSpecifier.ArrayRange;
import com.github.reload.services.storage.encoders.DataModel.ValueSpecifier;
import com.github.reload.services.storage.encoders.DictionaryModel.DictionaryValueBuilder;
import com.github.reload.services.storage.encoders.DictionaryModel.DictionaryValueSpecifier;
import com.github.reload.services.storage.encoders.FetchAnswer;
import com.github.reload.services.storage.encoders.FetchKindResponse;
import com.github.reload.services.storage.encoders.FetchRequest;
import com.github.reload.services.storage.encoders.SingleModel.SingleValueBuilder;
import com.github.reload.services.storage.encoders.StoreAnswer;
import com.github.reload.services.storage.encoders.StoreKindDataSpecifier;
import com.github.reload.services.storage.encoders.StoreKindResponse;
import com.github.reload.services.storage.encoders.StoreRequest;
import com.github.reload.services.storage.encoders.StoredData;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

/**
 * Helps a peer to send storage requests into the overlay
 * 
 */
@Component(value = StorageService.class, priority = 10)
public class StorageService {

	public static final ServiceIdentifier<StorageService> SERVICE_ID = new ServiceIdentifier<StorageService>(StorageService.class);

	private static final short REPLICA_NUMBER = 0;

	@Component
	private ComponentsContext ctx;

	@Component
	private Configuration conf;

	@Component
	private TopologyPlugin plugin;

	@Component
	private MessageRouter msgRouter;

	@Component
	private MessageBuilder msgBuilder;

	@Component
	private CryptoHelper<?> crypto;

	@CompStart
	private void loadController() {
		ctx.set(StorageController.class, new StorageController());
		ctx.startComponent(StorageController.class);
	}

	@Service
	private StorageService exportService() {
		return this;
	}

	/**
	 * @return the available data kinds
	 */
	public Set<DataKind> getDataKinds() {
		return conf.getDataKinds();
	}

	public PreparedData newPreparedData(DataKind kind) {
		return new PreparedData(kind);
	}

	public AccessParamsGenerator newParamsGenerator(DataKind kind) {
		return kind.getAccessPolicy().newParamsGenerator(ctx);
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
	public ListenableFuture<List<StoreKindResponse>> store(final ResourceID resourceId, PreparedData... preparedData) {
		Preconditions.checkNotNull(resourceId);
		Preconditions.checkNotNull(preparedData);

		if (resourceId.getData().length > plugin.getResourceIdLength())
			throw new IllegalArgumentException("Invalid resource-id length");

		final SettableFuture<List<StoreKindResponse>> storeFut = SettableFuture.create();

		if (preparedData.length == 0) {
			storeFut.set(Collections.<StoreKindResponse>emptyList());
			return storeFut;
		}

		Map<Long, StoreKindData> kindData = Maps.newHashMap();

		for (PreparedData prepared : preparedData) {
			StoredData data = prepared.build(ctx, resourceId);

			StoreKindData kd = kindData.get(prepared.getKind().getKindId());

			if (kd == null) {
				kd = new StoreKindData(prepared.getKind(), prepared.generation, new ArrayList<StoredData>());
				kindData.put(prepared.getKind().getKindId(), kd);
			}

			kd.getValues().add(data);
		}

		Message request = msgBuilder.newMessage(new StoreRequest(resourceId, REPLICA_NUMBER, new ArrayList<StoreKindData>(kindData.values())), new DestinationList(resourceId));

		ListenableFuture<Message> ansFut = msgRouter.sendRequestMessage(request);

		Futures.addCallback(ansFut, new FutureCallback<Message>() {

			@Override
			public void onSuccess(Message result) {
				StoreAnswer answer = (StoreAnswer) result.getContent();
				storeFut.set(answer.getResponses());
			}

			@Override
			public void onFailure(Throwable t) {
				if (t instanceof UnknownKindException)
					sendKindConfigUpdate(resourceId, ((UnknownKindException) t).getUnknownKinds());
				storeFut.setException(t);
			}
		});

		return storeFut;
	}

	protected void sendKindConfigUpdate(ResourceID resourceId, List<Long> unknownKinds) {
		// TODO Auto-generated method stub
	}

	public StoreKindDataSpecifier newDataSpecifier(DataKind kind) {
		return new StoreKindDataSpecifier(kind);
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
	public ListenableFuture<List<FetchKindResponse>> fetch(final ResourceID resourceId, StoreKindDataSpecifier... specifiers) {
		Preconditions.checkNotNull(resourceId);
		Preconditions.checkNotNull(specifiers);

		if (resourceId.getData().length > plugin.getResourceIdLength())
			throw new IllegalArgumentException("Invalid resource-id length");

		final SettableFuture<List<FetchKindResponse>> fetchFut = SettableFuture.create();

		Message message = msgBuilder.newMessage(new FetchRequest(resourceId, Arrays.asList(specifiers)), new DestinationList(resourceId));
		ListenableFuture<Message> ansFut = msgRouter.sendRequestMessage(message);

		Futures.addCallback(ansFut, new FutureCallback<Message>() {

			@Override
			public void onSuccess(Message result) {
				FetchAnswer answer = (FetchAnswer) result.getContent();
				try {
					for (FetchKindResponse r : answer.getResponses()) {
						verifyResponse(r, resourceId);
					}
					fetchFut.set(answer.getResponses());
				} catch (GeneralSecurityException e) {
					fetchFut.setException(e);
				}
			}

			@Override
			public void onFailure(Throwable t) {
				fetchFut.setException(t);
			}
		});

		return fetchFut;
	}

	private void verifyResponse(FetchKindResponse r, ResourceID resourceId) throws GeneralSecurityException {
		for (StoredData data : r.getValues()) {
			// Synthetic values are not authenticated
			if (data.getSignature().getIdentity().getIdentityType() != IdentityType.NONE) {
				ReloadCertificate reloCert = crypto.getCertificate(data.getSignature().getIdentity());
				// FIXME: handle certificate not found situation

				Certificate signerCert = reloCert.getOriginalCertificate();
				data.verify(signerCert.getPublicKey(), resourceId, r.getKind());
			}
		}
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
	public ListenableFuture<List<StoreKindResponse>> removeData(ResourceID resourceId, StoreKindDataSpecifier dataSpecifier) {
		Preconditions.checkNotNull(resourceId);
		Preconditions.checkNotNull(dataSpecifier);

		if (resourceId.getData().length > plugin.getResourceIdLength())
			throw new IllegalArgumentException("The resource-id exceeds the overlay allowed length of " + plugin.getResourceIdLength() + " bytes");

		DataKind kind = dataSpecifier.getKind();

		ValueSpecifier modelSpec = dataSpecifier.getValueSpecifier();

		List<PreparedData> preparedDatas = new ArrayList<PreparedData>();

		if (modelSpec instanceof ArrayValueSpecifier) {
			Set<Long> settedIndexes = new HashSet<Long>();
			for (ArrayRange r : ((ArrayValueSpecifier) modelSpec).getRanges()) {
				for (long i = r.getStartIndex(); i < r.getEndIndex(); i++) {
					PreparedData b = newPreparedData(kind);
					ArrayValueBuilder preparedVal = (ArrayValueBuilder) b.getValueBuilder();
					if (settedIndexes.contains(i)) {
						continue;
					}
					preparedVal.index(i);
					preparedVal.value(new byte[0], false);
					settedIndexes.add(i);

					preparedDatas.add(b);
				}
			}
		} else if (modelSpec instanceof DictionaryValueSpecifier) {
			for (byte[] k : ((DictionaryValueSpecifier) modelSpec).getKeys()) {
				PreparedData b = newPreparedData(kind);
				DictionaryValueBuilder preparedVal = (DictionaryValueBuilder) b.getValueBuilder();
				preparedVal.key(k);
				preparedVal.value(new byte[0], false);

				preparedDatas.add(b);
			}
		} else {
			PreparedData b = newPreparedData(kind);
			SingleValueBuilder preparedVal = (SingleValueBuilder) b.getValueBuilder();
			preparedVal.exists(false);

			preparedDatas.add(b);
		}

		for (PreparedData b : preparedDatas) {
			b.setLifeTime(0);
		}

		return store(resourceId, preparedDatas.toArray(new PreparedData[0]));
	}
}
