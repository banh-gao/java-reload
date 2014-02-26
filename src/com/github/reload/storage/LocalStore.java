package com.github.reload.storage;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import org.bouncycastle.asn1.ocsp.ResponseData;
import com.github.reload.Configuration;
import com.github.reload.DataKind;
import com.github.reload.message.NodeID;
import com.github.reload.message.ResourceID;
import com.github.reload.storage.StorageController.QueryType;
import com.github.reload.storage.data.StoredData;
import com.github.reload.storage.data.StoredDataSpecifier;
import com.github.reload.storage.data.StoredMetadata;
import com.github.reload.storage.errors.GenerationTooLowException;
import com.github.reload.storage.errors.NotFoundException;
import com.github.reload.storage.net.FetchKindResponse;
import com.github.reload.storage.net.FindAnswer;
import com.github.reload.storage.net.FindKindData;
import com.github.reload.storage.net.FindRequest;
import com.github.reload.storage.net.StatKindResponse;
import com.github.reload.storage.net.StoreKindData;
import com.github.reload.storage.net.StoreKindResponse;

/**
 * The map of the data stored locally. It stores the data which the peer is
 * responsible.
 * 
 * @author Daniel Zozin <zdenial@gmx.com>
 * 
 */
public class LocalStore {

	private final Configuration conf;

	private final ConcurrentMap<ResourceID, LocalKinds> storedResources = new ConcurrentHashMap<ResourceID, LocalKinds>();

	private final ScheduledExecutorService dataRemoverExecutor = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {

		@Override
		public Thread newThread(Runnable r) {
			Thread t = new Thread(r, "Expired data remover for " + context.getLocalId());
			t.setDaemon(true);
			return t;
		}
	});

	public LocalStore(Configuration conf) {
		context = context;
	}

	/**
	 * Perform controls on data and stores it
	 * 
	 * @throws StorageException
	 * 
	 */
	public List<StoreKindResponse> store(ResourceID resourceId, List<StoreKindData> data) throws StorageException {
		LocalKinds storedKinds = storedResources.get(resourceId);

		if (storedKinds == null) {
			storedKinds = new LocalKinds(resourceId, context, this);
			context.registerConfigUpdateListener(storedKinds);
		}

		List<StoreKindResponse> response = new ArrayList<StoreKindResponse>();
		List<StoreKindResponse> generTooLowResponses = new ArrayList<StoreKindResponse>();

		for (StoreKindData receivedData : data) {
			try {
				BigInteger newGeneration = storedKinds.add(receivedData);
				List<NodeID> replicaIds = context.getTopologyPlugin().onReplicateData(resourceId, receivedData);
				response.add(new StoreKindResponse(receivedData.getKind(), newGeneration, replicaIds));
			} catch (GenerationTooLowException e) {
				// Rollback
				storedKinds.remove(receivedData);
				generTooLowResponses.add(new StoreKindResponse(receivedData.getKind(), e.getGeneration(), new ArrayList<NodeID>()));
			}
		}

		if (generTooLowResponses.size() > 0)
			throw new GenerationTooLowException(generTooLowResponses);

		storedResources.put(resourceId, storedKinds);

		return response;
	}

	public Map<ResourceID, LocalKinds> getStoredResources() {
		return new HashMap<ResourceID, LocalKinds>(storedResources);
	}

	public void removeResource(ResourceID resourceId) {
		storedResources.remove(resourceId);
	}

	public int getSize() {
		return storedResources.size();
	}

	@SuppressWarnings("unchecked")
	public <T extends KindResponse<?>> List<T> query(ResourceID resourceId, List<StoredDataSpecifier> specifiers, QueryType queryType) throws NotFoundException {
		LocalKinds storedKinds = getStoredKinds(resourceId);

		List<KindResponse<? extends ResponseData>> out = new ArrayList<KindResponse<? extends ResponseData>>();

		for (StoredDataSpecifier spec : specifiers) {
			LocalKindData data = storedKinds.get(spec.getDataKind().getKindId());

			if (data != null && data.size() > 0) {
				DataKind kind = data.getKind();
				BigInteger genCounter = data.getGeneration();

				if (queryType == QueryType.FETCH) {
					List<StoredData> values = data.getMatchingValues(spec, queryType);
					out.add(new FetchKindResponse(kind, genCounter, values));
				} else {
					List<StoredMetadata> values = data.getMatchingValues(spec, queryType);
					out.add(new StatKindResponse(kind, genCounter, values));
				}
			} else {
				if (queryType == QueryType.FETCH) {
					List<StoredData> values = new ArrayList<StoredData>();
					values.add(StoredData.getNonExistentData(spec.getDataKind()));
					out.add(new FetchKindResponse(spec.getDataKind(), BigInteger.ZERO, values));
				}
			}
		}

		return (List<T>) out;
	}

	private LocalKinds getStoredKinds(ResourceID resourceId) throws NotFoundException {
		LocalKinds storedKinds = storedResources.get(resourceId);

		if (storedKinds == null)
			throw new NotFoundException("Requested resource not found");

		if (storedKinds.size() == 0) {
			storedResources.remove(resourceId);
			throw new NotFoundException("Requested resource not found");
		}
		return storedKinds;
	}

	public FindAnswer find(FindRequest req) throws NotFoundException {
		ResourceID requestedId = req.getResourceId();

		if (!context.getTopologyPlugin().isThisPeerResponsible(requestedId))
			throw new NotFoundException("Node not responsible for requested resource");

		Set<KindId> requestedKinds = new LinkedHashSet<KindId>(req.getKinds());
		requestedKinds.retainAll(context.getConfiguration().getDataKindIds());

		Map<KindId, ResourceID> results = new LinkedHashMap<KindId, ResourceID>();

		for (Entry<ResourceID, LocalKinds> e : storedResources.entrySet()) {
			ResourceID curResId = e.getKey();
			LocalKinds kinds = e.getValue();

			for (KindId requestedKind : requestedKinds) {
				LocalKindData skd = kinds.get(requestedKind);

				ResourceID lastCloserId = results.get(requestedKind);

				if (lastCloserId == null) {
					results.put(requestedKind, skd.resourceId);
				} else {
					List<ResourceID> ids = new ArrayList<ResourceID>();
					ids.add(skd.resourceId);
					ids.add(curResId);

					results.put(requestedKind, context.getTopologyPlugin().getCloserId(requestedId, ids));
				}
			}

		}

		List<FindKindData> answers = new ArrayList<FindKindData>();
		for (Entry<KindId, ResourceID> e : results.entrySet()) {
			answers.add(new FindKindData(e.getKey(), e.getValue()));
		}

		return new FindAnswer(answers);
	}

	ScheduledExecutorService getDataRemoverExecutor() {
		return dataRemoverExecutor;
	}

	public BigInteger getUsedMemory() {
		BigInteger usedMemory = BigInteger.ZERO;
		for (LocalKinds k : storedResources.values()) {
			for (LocalKindData data : k.getAll()) {
				for (StoredData d : data.getDataValues()) {
					usedMemory = usedMemory.add(BigInteger.valueOf(d.getValue().getSize()));
				}
			}
		}

		return usedMemory;
	}
}