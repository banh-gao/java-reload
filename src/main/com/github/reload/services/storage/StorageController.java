package com.github.reload.services.storage;

import java.security.GeneralSecurityException;
import java.util.List;
import com.github.reload.components.ComponentsContext;
import com.github.reload.components.ComponentsContext.CompStart;
import com.github.reload.components.ComponentsRepository.Component;
import com.github.reload.components.MessageHandlersManager.MessageHandler;
import com.github.reload.conf.Configuration;
import com.github.reload.crypto.CryptoHelper;
import com.github.reload.net.MessageRouter;
import com.github.reload.net.encoders.Message;
import com.github.reload.net.encoders.MessageBuilder;
import com.github.reload.net.encoders.content.ContentType;
import com.github.reload.net.encoders.content.Error.ErrorMessageException;
import com.github.reload.net.encoders.content.Error.ErrorType;
import com.github.reload.net.encoders.header.NodeID;
import com.github.reload.net.encoders.secBlock.SignerIdentity;
import com.github.reload.routing.TopologyPlugin;
import com.github.reload.services.storage.encoders.FetchAnswer;
import com.github.reload.services.storage.encoders.FetchRequest;
import com.github.reload.services.storage.encoders.FindAnswer;
import com.github.reload.services.storage.encoders.FindRequest;
import com.github.reload.services.storage.encoders.StatAnswer;
import com.github.reload.services.storage.encoders.StatRequest;
import com.github.reload.services.storage.encoders.StoreAnswer;
import com.github.reload.services.storage.encoders.StoreKindResponse;
import com.github.reload.services.storage.encoders.StoreRequest;

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
	private CryptoHelper<?> crypto;

	@Component
	private ComponentsContext ctx;

	private LocalStore localStore;

	@CompStart
	public void start() {
		localStore = new LocalStore();
	}

	@MessageHandler(ContentType.STORE_REQ)
	private void handleStoreRequest(Message requestMessage) {
		StoreRequest req = (StoreRequest) requestMessage.getContent();

		boolean isReplica = req.getReplicaNumber() != 0;

		if (plugin.isLocalPeerValidStorage(req.getResourceId(), isReplica) == false) {
			router.sendError(requestMessage.getHeader(), ErrorType.FORBITTEN, "Node not responsible to store requested resource");
			return;
		}

		// FIXME: Check generation counter for replica stores

		SignerIdentity senderIdentity = requestMessage.getSecBlock().getSignature().getIdentity();

		List<NodeID> replicaNodes = plugin.getReplicaNodes(req.getResourceId());

		List<StoreKindResponse> response;
		try {
			response = localStore.store(req.getResourceId(), req.getKindData(), senderIdentity, isReplica, replicaNodes, ctx);
		} catch (GeneralSecurityException e) {
			router.sendError(requestMessage.getHeader(), ErrorType.FORBITTEN, "Invalid data signature");
			return;
		} catch (ErrorMessageException e) {
			router.sendError(requestMessage.getHeader(), e.getType(), e.getInfo());
			return;
		}

		for (StoreKindResponse resp : response) {
			plugin.requestReplication(req.getResourceId(), resp.getKind());
		}

		StoreAnswer answer = new StoreAnswer(response);

		router.sendAnswer(requestMessage.getHeader(), answer);

		cleanupStore();
	}

	@MessageHandler(ContentType.FETCH_REQ)
	private void handleFetchRequest(Message requestMessage) {
		FetchRequest req = (FetchRequest) requestMessage.getContent();
		FetchAnswer answer = new FetchAnswer(localStore.fetch(req.getResourceId(), req.getSpecifiers()));
		router.sendAnswer(requestMessage.getHeader(), answer);

		cleanupStore();
	}

	@MessageHandler(ContentType.STAT_REQ)
	private void handleStatRequest(Message requestMessage) {
		StatRequest req = (StatRequest) requestMessage.getContent();
		StatAnswer answer = new StatAnswer(localStore.stat(req.getResourceId(), req.getSpecifiers()));
		router.sendAnswer(requestMessage.getHeader(), answer);

		cleanupStore();
	}

	@MessageHandler(ContentType.FIND_REQ)
	private void handleFindRequest(Message requestMessage) {
		FindRequest req = (FindRequest) requestMessage.getContent();
		FindAnswer answer = new FindAnswer(localStore.find(req.getResourceId(), req.getKinds(), plugin));
		router.sendAnswer(requestMessage.getHeader(), answer);

		cleanupStore();
	}

	private void cleanupStore() {
		// TODO: perform store cleanup
	}
}
