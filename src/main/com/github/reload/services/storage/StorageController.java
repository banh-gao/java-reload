package com.github.reload.services.storage;

import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.github.reload.components.ComponentsContext;
import com.github.reload.components.ComponentsRepository.Component;
import com.github.reload.components.MessageHandlersManager.MessageHandler;
import com.github.reload.conf.Configuration;
import com.github.reload.crypto.CryptoHelper;
import com.github.reload.crypto.Keystore;
import com.github.reload.crypto.ReloadCertificate;
import com.github.reload.net.MessageRouter;
import com.github.reload.net.encoders.Message;
import com.github.reload.net.encoders.MessageBuilder;
import com.github.reload.net.encoders.content.ContentType;
import com.github.reload.net.encoders.content.Error.ErrorMessageException;
import com.github.reload.net.encoders.content.Error.ErrorType;
import com.github.reload.net.encoders.header.NodeID;
import com.github.reload.net.encoders.header.ResourceID;
import com.github.reload.net.encoders.secBlock.Signature;
import com.github.reload.net.encoders.secBlock.SignerIdentity;
import com.github.reload.net.encoders.secBlock.SignerIdentity.IdentityType;
import com.github.reload.routing.TopologyPlugin;
import com.github.reload.services.storage.encoders.DataModel.ValueSpecifier;
import com.github.reload.services.storage.encoders.FetchAnswer;
import com.github.reload.services.storage.encoders.FetchKindResponse;
import com.github.reload.services.storage.encoders.FetchRequest;
import com.github.reload.services.storage.encoders.FindAnswer;
import com.github.reload.services.storage.encoders.FindKindData;
import com.github.reload.services.storage.encoders.FindRequest;
import com.github.reload.services.storage.encoders.StatAnswer;
import com.github.reload.services.storage.encoders.StatKindResponse;
import com.github.reload.services.storage.encoders.StatRequest;
import com.github.reload.services.storage.encoders.StoreAnswer;
import com.github.reload.services.storage.encoders.StoreKindDataSpecifier;
import com.github.reload.services.storage.encoders.StoreKindResponse;
import com.github.reload.services.storage.encoders.StoreRequest;
import com.github.reload.services.storage.encoders.StoredData;
import com.github.reload.services.storage.encoders.StoredMetadata;
import com.google.common.base.Optional;

/**
 * Process incoming storage requests and accesses local storage
 * 
 */
class StorageController {

	@Component
	private Configuration conf;

	@Component
	private TopologyPlugin plugin;

	@Component
	private MessageBuilder msgBuilder;

	@Component
	private MessageRouter router;

	@Component
	private ComponentsContext ctx;

	@Component
	private DataStorage storage;

	@MessageHandler(ContentType.STORE_REQ)
	private void handleStoreRequest(Message requestMessage) {
		StoreRequest req = (StoreRequest) requestMessage.getContent();

		boolean isReplica = req.getReplicaNumber() != 0;

		if (plugin.isLocalPeerValidStorage(req.getResourceId(), isReplica) == false) {
			router.sendError(requestMessage.getHeader(), ErrorType.FORBITTEN, "Node not responsible to store requested resource");
			return;
		}

		SignerIdentity senderIdentity = requestMessage.getSecBlock().getSignature().getIdentity();

		List<NodeID> replicaNodes = plugin.getReplicaNodes(req.getResourceId());

		List<StoreKindResponse> response;
		try {
			response = store(req.getResourceId(), req.getKindData(), senderIdentity, isReplica, replicaNodes);
		} catch (GeneralSecurityException e) {
			router.sendError(requestMessage.getHeader(), ErrorType.FORBITTEN, "Invalid data signature");
			return;
		} catch (ErrorMessageException e) {
			router.sendError(requestMessage.getHeader(), e.getType(), e.getInfo());
			return;
		}

		StoreAnswer answer = new StoreAnswer(response);

		router.sendAnswer(requestMessage.getHeader(), answer);

		plugin.requestReplication(req.getResourceId());
	}

	private List<StoreKindResponse> store(ResourceID resourceId, Collection<StoreKindData> data, SignerIdentity senderIdentity, boolean isReplica, List<NodeID> replicaNodes) throws GeneralSecurityException, ErrorMessageException {
		List<StoreKindResponse> response = new ArrayList<StoreKindResponse>();
		List<StoreKindResponse> generTooLowResponses = new ArrayList<StoreKindResponse>();

		Optional<Map<Long, StoreKindData>> oldStoredResource = storage.getResource(resourceId);

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

				Optional<ReloadCertificate> storerCert = ctx.get(Keystore.class).getCertificate(storerIdentity);

				if (!storerCert.isPresent())
					throw new GeneralSecurityException("Storer certificate not available");

				d.verify(storerCert.get().getOriginalCertificate().getPublicKey(), resourceId, receivedData.getKind());
			}

			if (oldStoredKind.isPresent() && !checkGeneration(receivedData, oldStoredKind.get(), isReplica)) {
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
		storage.put(resourceId, tempStore);

		return response;
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

	private boolean checkGeneration(StoreKindData receivedData, StoreKindData oldStoredKind, boolean isReplica) {
		BigInteger rcvGen = receivedData.getGeneration();

		// Generation for replica stores needs only to be not zero
		if (isReplica)
			return !rcvGen.equals(BigInteger.ZERO);

		if (rcvGen.equals(BigInteger.ZERO))
			return true;

		BigInteger oldGeneration = oldStoredKind.getGeneration();

		// True (valid request) only if the new data generation is greater than
		// old generation
		return receivedData.getGeneration().compareTo(oldGeneration) > 0;
	}

	@MessageHandler(ContentType.FETCH_REQ)
	private void handleFetchRequest(Message requestMessage) {
		FetchRequest req = (FetchRequest) requestMessage.getContent();
		FetchAnswer answer;
		try {
			answer = new FetchAnswer(fetch(req.getResourceId(), req.getSpecifiers()));
		} catch (ErrorMessageException e) {
			router.sendError(requestMessage.getHeader(), e.getType(), e.getInfo());
			return;
		}
		router.sendAnswer(requestMessage.getHeader(), answer);
	}

	private List<FetchKindResponse> fetch(ResourceID resourceId, List<StoreKindDataSpecifier> specifiers) throws ErrorMessageException {
		List<FetchKindResponse> out = new ArrayList<FetchKindResponse>();

		Optional<Map<Long, StoreKindData>> resource = storage.getResource(resourceId);

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

	@MessageHandler(ContentType.STAT_REQ)
	private void handleStatRequest(Message requestMessage) {
		StatRequest req = (StatRequest) requestMessage.getContent();
		StatAnswer answer;
		try {
			answer = new StatAnswer(stat(req.getResourceId(), req.getSpecifiers()));
		} catch (ErrorMessageException e) {
			router.sendError(requestMessage.getHeader(), e.getType(), e.getInfo());
			return;
		}
		router.sendAnswer(requestMessage.getHeader(), answer);
	}

	private List<StatKindResponse> stat(ResourceID resourceId, List<StoreKindDataSpecifier> specifiers) throws ErrorMessageException {
		List<StatKindResponse> out = new ArrayList<StatKindResponse>();

		Optional<Map<Long, StoreKindData>> resource = storage.getResource(resourceId);

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

	@MessageHandler(ContentType.FIND_REQ)
	private void handleFindRequest(Message requestMessage) {
		FindRequest req = (FindRequest) requestMessage.getContent();
		FindAnswer answer = new FindAnswer(find(req.getResourceId(), req.getKinds()));
		router.sendAnswer(requestMessage.getHeader(), answer);
	}

	private Set<FindKindData> find(ResourceID reqResId, Set<DataKind> reqKinds) {
		Set<FindKindData> out = new HashSet<FindKindData>();

		for (DataKind k : reqKinds) {

			Collection<ResourceID> resources = storage.getResourcesByKind(k.getKindId());

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
