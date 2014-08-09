package com.github.reload.services.storage;

import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.github.reload.components.ComponentsContext;
import com.github.reload.crypto.CryptoHelper;
import com.github.reload.crypto.ReloadCertificate;
import com.github.reload.net.encoders.content.Error.ErrorMessageException;
import com.github.reload.net.encoders.content.Error.ErrorType;
import com.github.reload.net.encoders.header.NodeID;
import com.github.reload.net.encoders.header.ResourceID;
import com.github.reload.net.encoders.secBlock.Signature;
import com.github.reload.net.encoders.secBlock.SignerIdentity;
import com.github.reload.services.storage.encoders.DataModel.ValueSpecifier;
import com.github.reload.services.storage.encoders.FetchKindResponse;
import com.github.reload.services.storage.encoders.StatKindResponse;
import com.github.reload.services.storage.encoders.StoreAnswer;
import com.github.reload.services.storage.encoders.StoreKindDataSpecifier;
import com.github.reload.services.storage.encoders.StoreKindResponse;
import com.github.reload.services.storage.encoders.StoredData;
import com.github.reload.services.storage.encoders.StoredMetadata;
import com.google.common.collect.Maps;

/**
 * The map of the data stored locally. It stores the data which the peer is
 * responsible.
 * 
 */
public class LocalStore {

	private final Map<KindKey, StoreKindData> storedResources = Maps.newConcurrentMap();

	/**
	 * Perform controls on data and stores it
	 * 
	 * @throws GenerationTooLowException
	 * 
	 * @throws StorageException
	 * 
	 */
	public List<StoreKindResponse> store(ResourceID resourceId, List<StoreKindData> data, SignerIdentity senderIdentity, boolean isReplica, ComponentsContext ctx) throws GeneralSecurityException, ErrorMessageException {
		List<StoreKindResponse> response = new ArrayList<StoreKindResponse>();
		List<StoreKindResponse> generTooLowResponses = new ArrayList<StoreKindResponse>();

		Map<KindKey, StoreKindData> tempStore = new HashMap<LocalStore.KindKey, StoreKindData>(data.size());

		for (StoreKindData receivedData : data) {

			KindKey key = new KindKey(resourceId, receivedData.getKind().getKindId());

			StoreKindData oldStoredKind = storedResources.get(key);

			// Verify request validity, storage policy and signature
			for (StoredData d : receivedData.getValues()) {
				if (oldStoredKind != null)
					checkValidReplace(oldStoredKind, d);

				SignerIdentity storerIdentity = d.getSignature().getIdentity();

				// Perform policy checks for storer node
				receivedData.getKind().getAccessPolicy().accept(resourceId, receivedData.getKind(), d, storerIdentity, ctx);

				// If the store is not a replica (it is a normal store request
				// from a peer), perform policy checks also for the node that
				// sends the store message
				if (!isReplica)
					receivedData.getKind().getAccessPolicy().accept(resourceId, receivedData.getKind(), d, senderIdentity, ctx);

				ReloadCertificate storerCert = ctx.get(CryptoHelper.class).getCertificate(storerIdentity);

				d.verify(storerCert.getOriginalCertificate().getPublicKey(), resourceId, receivedData.getKind());
			}

			// TODO: get replica nodes
			List<NodeID> replicas = Collections.emptyList();

			if (oldStoredKind != null && !checkGeneration(receivedData, oldStoredKind)) {
				generTooLowResponses.add(new StoreKindResponse(receivedData.getKind(), oldStoredKind.getGeneration(), replicas));
				continue;
			}

			// Increase stored data generation by one
			receivedData.generation = receivedData.generation.add(BigInteger.ONE);

			tempStore.put(key, receivedData);

			response.add(new StoreKindResponse(receivedData.getKind(), receivedData.getGeneration(), replicas));
		}

		if (generTooLowResponses.size() > 0)
			throw new GenerationTooLowException(new StoreAnswer(generTooLowResponses));

		// Store incoming data in the effettive storage
		storedResources.putAll(tempStore);

		// TODO: store cleanup

		return response;
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

	public Map<KindKey, StoreKindData> getStoredResources() {
		return Collections.unmodifiableMap(storedResources);
	}

	public void removeResource(ResourceID resourceId) {
		storedResources.remove(resourceId);
	}

	public int getSize() {
		return storedResources.size();
	}

	public List<FetchKindResponse> fetch(ResourceID resourceId, List<StoreKindDataSpecifier> specifiers) {

		List<FetchKindResponse> out = new ArrayList<FetchKindResponse>();

		for (StoreKindDataSpecifier spec : specifiers) {
			KindKey key = new KindKey(resourceId, spec.getKind().getKindId());

			StoreKindData kindData = storedResources.get(key);

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

	public List<StatKindResponse> stat(ResourceID resourceId, List<StoreKindDataSpecifier> specifiers) {
		List<StatKindResponse> out = new ArrayList<StatKindResponse>();

		for (StoreKindDataSpecifier spec : specifiers) {
			KindKey key = new KindKey(resourceId, spec.getKind().getKindId());

			StoreKindData kindData = storedResources.get(key);

			if (kindData == null) {
				continue;
			}

			// If the generation in the request corresponds to the last
			// value we don't need to resend the same content
			if (kindData.getGeneration().equals(spec.getGeneration())) {
				continue;
			}

			List<StoredMetadata> matchingData = new ArrayList<StoredMetadata>();

			for (StoredData d : kindData.getValues()) {
				if (spec.getValueSpecifier().isMatching(d.getValue())) {
					matchingData.add(d.getMetadata(spec.getKind(), CryptoHelper.OVERLAY_HASHALG));
				}
			}

			out.add(new StatKindResponse(spec.getKind(), kindData.getGeneration(), matchingData));
		}

		return out;
	}

	public static class KindKey {

		public final ResourceID resId;
		public final long kindId;

		public KindKey(ResourceID resId, long kindId) {
			this.resId = resId;
			this.kindId = kindId;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + (int) (kindId ^ (kindId >>> 32));
			result = prime * result + ((resId == null) ? 0 : resId.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			KindKey other = (KindKey) obj;
			if (kindId != other.kindId)
				return false;
			if (resId == null) {
				if (other.resId != null)
					return false;
			} else if (!resId.equals(other.resId))
				return false;
			return true;
		}
	}
}