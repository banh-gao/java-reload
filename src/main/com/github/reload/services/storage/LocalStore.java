package com.github.reload.services.storage;

import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.github.reload.components.ComponentsContext;
import com.github.reload.components.ComponentsRepository.Component;
import com.github.reload.crypto.CryptoHelper;
import com.github.reload.crypto.ReloadCertificate;
import com.github.reload.net.encoders.content.Error.ErrorMessageException;
import com.github.reload.net.encoders.content.Error.ErrorType;
import com.github.reload.net.encoders.header.NodeID;
import com.github.reload.net.encoders.header.ResourceID;
import com.github.reload.net.encoders.secBlock.Signature;
import com.github.reload.net.encoders.secBlock.SignerIdentity;
import com.github.reload.net.encoders.secBlock.SignerIdentity.IdentityType;
import com.github.reload.routing.TopologyPlugin;
import com.github.reload.services.storage.encoders.DataModel.ValueSpecifier;
import com.github.reload.services.storage.encoders.FetchKindResponse;
import com.github.reload.services.storage.encoders.FindKindData;
import com.github.reload.services.storage.encoders.StatKindResponse;
import com.github.reload.services.storage.encoders.StoreAnswer;
import com.github.reload.services.storage.encoders.StoreKindDataSpecifier;
import com.github.reload.services.storage.encoders.StoreKindResponse;
import com.github.reload.services.storage.encoders.StoredData;
import com.github.reload.services.storage.encoders.StoredMetadata;
import com.google.common.base.Optional;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

/**
 * The data stored locally. It stores the data the peer is
 * responsible for.
 * 
 */
@Component(LocalStore.class)
public class LocalStore {

	@Component
	private ComponentsContext ctx;

	@Component
	private TopologyPlugin plugin;

	private final Map<ResourceID, Map<Long, StoreKindData>> storedResources = Maps.newConcurrentMap();

	private final Multimap<Long, ResourceID> storedKinds = ArrayListMultimap.create();

	/**
	 * Perform controls on data and stores it
	 * 
	 */
	public List<StoreKindResponse> store(ResourceID resourceId, Collection<StoreKindData> data, SignerIdentity senderIdentity, boolean isReplica, List<NodeID> replicaNodes) throws GeneralSecurityException, ErrorMessageException {
		List<StoreKindResponse> response = new ArrayList<StoreKindResponse>();
		List<StoreKindResponse> generTooLowResponses = new ArrayList<StoreKindResponse>();

		Optional<Map<Long, StoreKindData>> oldStoredResource = getResource(resourceId);

		Map<Long, StoreKindData> tempStore = new HashMap<Long, StoreKindData>(data.size());

		Set<Long> requestKinds = new HashSet<Long>();

		for (StoreKindData receivedData : data) {

			DataKind kind = receivedData.getKind();

			requestKinds.add(kind.getKindId());

			Optional<StoreKindData> oldStoredKind = Optional.absent();

			if (oldStoredResource.isPresent())
				oldStoredKind = Optional.fromNullable(oldStoredResource.get().get(kind.getKindId()));

			if (receivedData.getValues().size() > kind.getAttribute(DataKind.MAX_COUNT, DataKind.MAX_COUNT_DEFAULT))
				throw new ErrorMessageException(ErrorType.DATA_TOO_LARGE, "Stored data exceeds maximum number of values for this kind");

			// Verify request validity, storage policy and signature
			for (StoredData d : receivedData.getValues()) {

				if (d.getValue().getSize() > kind.getAttribute(DataKind.MAX_SIZE, DataKind.MAX_SIZE_DEFAULT))
					throw new ErrorMessageException(ErrorType.DATA_TOO_LARGE, "Stored data exceeds maximum size for this kind");

				if (oldStoredKind.isPresent())
					checkValidReplace(oldStoredKind.get(), d);

				SignerIdentity storerIdentity = d.getSignature().getIdentity();

				if (storerIdentity.getIdentityType() == IdentityType.NONE)
					throw new ErrorMessageException(ErrorType.FORBITTEN, "NONE identity type not allowed");

				// Perform policy checks for storer node
				receivedData.getKind().getAccessPolicy().accept(resourceId, receivedData.getKind(), d, storerIdentity, ctx);

				// If the store is not a replica (it is a normal store request
				// from a peer), perform policy checks also for the node that
				// sends the store message
				if (!isReplica)
					kind.getAccessPolicy().accept(resourceId, kind, d, senderIdentity, ctx);

				ReloadCertificate storerCert = ctx.get(CryptoHelper.class).getCertificate(storerIdentity);

				d.verify(storerCert.getOriginalCertificate().getPublicKey(), resourceId, receivedData.getKind());
			}

			if (oldStoredKind.isPresent() && !checkGeneration(receivedData, oldStoredKind.get())) {
				generTooLowResponses.add(new StoreKindResponse(kind, oldStoredKind.get().getGeneration(), replicaNodes));
				continue;
			}

			// Increase stored data generation by one
			if (oldStoredKind.isPresent())
				receivedData.generation = oldStoredKind.get().getGeneration().add(BigInteger.ONE);
			else
				receivedData.generation = receivedData.generation.add(BigInteger.ONE);

			tempStore.put(kind.getKindId(), receivedData);

			response.add(new StoreKindResponse(kind, receivedData.getGeneration(), replicaNodes));
		}

		if (generTooLowResponses.size() > 0)
			throw new GenerationTooLowException(new StoreAnswer(generTooLowResponses));

		// Store incoming data in the effettive storage
		storedResources.put(resourceId, tempStore);

		updateKindToResource(requestKinds, resourceId);

		return response;
	}

	private void updateKindToResource(Set<Long> kinds, ResourceID resId) {
		for (Long k : kinds)
			storedKinds.put(k, resId);
	}

	private boolean checkGeneration(StoreKindData receivedData, StoreKindData oldStoredKind) {
		if (receivedData.getGeneration().compareTo(BigInteger.ZERO) == 0)
			return true;

		BigInteger oldGeneration = oldStoredKind.getGeneration();

		// True (valid request) only if the new data generation is greater than
		// old generation
		return receivedData.getGeneration().compareTo(oldGeneration) > 0;
	}

	private void checkValidReplace(StoreKindData oldKindData, StoredData newData) throws ErrorMessageException {
		if (oldKindData == null)
			return;

		ValueSpecifier spec = newData.getValue().getMatchingSpecifier();
		StoredData oldValue = getMatchingData(oldKindData, spec).get(0);

		if (oldValue == null)
			return;

		// Check storage time for values that will be replaced
		if (newData.getStorageTime().compareTo(oldValue.getStorageTime()) < 0)
			throw new ErrorMessageException(ErrorType.DATA_TOO_OLD);
	}

	public Map<ResourceID, Map<Long, StoreKindData>> getStoredResources() {
		return Collections.unmodifiableMap(storedResources);
	}

	public Optional<Map<Long, StoreKindData>> getResource(ResourceID resId) {
		return Optional.fromNullable(storedResources.get(resId));
	}

	public void removeResource(ResourceID resourceId) {
		storedResources.remove(resourceId);
	}

	public int getSize() {
		return storedResources.size();
	}

	public List<FetchKindResponse> fetch(ResourceID resourceId, List<StoreKindDataSpecifier> specifiers) throws ErrorMessageException {

		List<FetchKindResponse> out = new ArrayList<FetchKindResponse>();

		Optional<Map<Long, StoreKindData>> resource = getResource(resourceId);

		if (!resource.isPresent())
			throw new ErrorMessageException(ErrorType.NOT_FOUND, String.format("Resource %s not found", resourceId));

		for (StoreKindDataSpecifier spec : specifiers) {

			StoreKindData kindData = resource.get().get(spec.getKind().getKindId());

			if (kindData == null) {
				continue;
			}

			// If the generation in the request corresponds to the last
			// value we don't need to resend the same content
			if (kindData.getGeneration().equals(spec.getGeneration())) {
				continue;
			}

			List<StoredData> matchingData = getMatchingData(kindData, spec.getValueSpecifier());

			if (matchingData.isEmpty()) {
				matchingData.add(getNonExistentData(spec.getKind()));
			}

			out.add(new FetchKindResponse(spec.getKind(), kindData.getGeneration(), matchingData));
		}

		return out;
	}

	/**
	 * Returns the data in the given StoreKindData that matches the given
	 * ValueSpecifier
	 * 
	 * @param kindData
	 * @param spec
	 * @return
	 */
	private List<StoredData> getMatchingData(StoreKindData kindData, ValueSpecifier spec) {
		List<StoredData> matchingData = new ArrayList<StoredData>();

		for (StoredData d : kindData.getValues()) {
			if (spec.isMatching(d.getValue())) {
				matchingData.add(d);
			}
		}

		return matchingData;
	}

	private StoredData getNonExistentData(DataKind kind) {
		return new StoredData(BigInteger.ZERO, 0, kind.getDataModel().getNonExistentValue(), Signature.EMPTY_SIGNATURE);
	}

	public List<StatKindResponse> stat(ResourceID resourceId, List<StoreKindDataSpecifier> specifiers) throws ErrorMessageException {
		List<StatKindResponse> out = new ArrayList<StatKindResponse>();

		Optional<Map<Long, StoreKindData>> resource = getResource(resourceId);

		if (!resource.isPresent())
			throw new ErrorMessageException(ErrorType.NOT_FOUND, String.format("Resource %s not found", resourceId));

		for (StoreKindDataSpecifier spec : specifiers) {

			StoreKindData kindData = resource.get().get(spec.getKind().getKindId());

			if (kindData == null) {
				continue;
			}

			// If the generation in the request corresponds to the last
			// value we don't need to resend the same content
			if (kindData.getGeneration().equals(spec.getGeneration())) {
				continue;
			}

			List<StoredMetadata> matchingData = new ArrayList<StoredMetadata>();

			for (StoredData d : getMatchingData(kindData, spec.getValueSpecifier())) {
				matchingData.add(d.getMetadata(spec.getKind(), CryptoHelper.OVERLAY_HASHALG));
			}

			out.add(new StatKindResponse(spec.getKind(), kindData.getGeneration(), matchingData));
		}

		return out;
	}

	public Set<FindKindData> find(ResourceID reqResId, Set<DataKind> reqKinds) {

		Set<FindKindData> out = new HashSet<FindKindData>();

		for (DataKind k : reqKinds) {

			Collection<ResourceID> resources = storedKinds.get(k.getKindId());

			ResourceID resId;

			if (resources.isEmpty())
				resId = plugin.getResourceId(new byte[0]);
			else
				resId = plugin.getCloserId(reqResId, resources);

			out.add(new FindKindData(k, resId));
		}

		return out;
	}
}