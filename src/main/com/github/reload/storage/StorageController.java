package com.github.reload.storage;

import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Map;
import com.github.reload.Components.Component;
import com.github.reload.Components.MessageHandler;
import com.github.reload.Components.start;
import com.github.reload.conf.Configuration;
import com.github.reload.crypto.CryptoHelper;
import com.github.reload.crypto.ReloadCertificate;
import com.github.reload.net.MessageRouter;
import com.github.reload.net.encoders.Message;
import com.github.reload.net.encoders.MessageBuilder;
import com.github.reload.net.encoders.content.Content;
import com.github.reload.net.encoders.content.ContentType;
import com.github.reload.net.encoders.content.errors.Error;
import com.github.reload.net.encoders.content.errors.ErrorType;
import com.github.reload.net.encoders.content.storage.FetchAnswer;
import com.github.reload.net.encoders.content.storage.FetchRequest;
import com.github.reload.net.encoders.content.storage.StatAnswer;
import com.github.reload.net.encoders.content.storage.StatRequest;
import com.github.reload.net.encoders.content.storage.StoreAnswer;
import com.github.reload.net.encoders.content.storage.StoreKindData;
import com.github.reload.net.encoders.content.storage.StoreKindResponse;
import com.github.reload.net.encoders.content.storage.StoreRequest;
import com.github.reload.net.encoders.header.NodeID;
import com.github.reload.net.encoders.header.ResourceID;
import com.github.reload.routing.TopologyPlugin;
import com.github.reload.storage.LocalStore.KindKey;
import com.github.reload.storage.errors.GenerationTooLowException;

/**
 * Elaborate the storage messages and is responsible for local storage
 * management
 * 
 */
@Component(StorageController.COMPNAME)
class StorageController {

	public static final String COMPNAME = "com.github.reload.storage.StorageController";

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

	private LocalStore localStore;

	@start
	public void start() {
		localStore = new LocalStore(plugin);
	}

	@MessageHandler(ContentType.STORE_REQ)
	private void handleStoreRequest(Message requestMessage) {
		StoreRequest req = (StoreRequest) requestMessage.getContent();

		if (req.getReplicaNumber() == 0 && !plugin.isThisPeerResponsible(req.getResourceId())) {
			sendAnswer(requestMessage, new Error(ErrorType.NOT_FOUND, "Node not responsible for requested resource"));
			return;
		}

		if (req.getReplicaNumber() > 0 && !plugin.isThisNodeValidReplicaFor(requestMessage)) {
			sendAnswer(requestMessage, new Error(ErrorType.NOT_FOUND, "Node not valid replica for requested resource"));
			return;
		}

		NodeID sender = requestMessage.getHeader().getSenderId();

		ReloadCertificate senderCert = crypto.getCertificate(sender);

		List<StoreKindResponse> response;
		try {
			response = localStore.store(req.getResourceId(), req.getKindData(), senderCert.getOriginalCertificate().getPublicKey());
		} catch (GeneralSecurityException e) {
			sendAnswer(requestMessage, new Error(ErrorType.FORBITTEN, "Invalid data signature"));
			return;
		} catch (GenerationTooLowException e) {
			sendAnswer(requestMessage, new Error(ErrorType.GEN_COUNTER_TOO_LOW, e.getMessage()));
			return;
		}
		StoreAnswer answer = new StoreAnswer(response);

		sendAnswer(requestMessage, answer);
	}

	@MessageHandler(ContentType.FETCH_REQ)
	private void handleFetchRequest(Message requestMessage) {
		FetchRequest req = (FetchRequest) requestMessage.getContent();
		FetchAnswer answer = new FetchAnswer(localStore.fetch(req.getResourceId(), req.getSpecifiers()));
		sendAnswer(requestMessage, answer);
	}

	@MessageHandler(ContentType.STAT_REQ)
	private void handleStatRequest(Message requestMessage) {
		StatRequest req = (StatRequest) requestMessage.getContent();
		Content answer = new StatAnswer(localStore.stat(req.getResourceId(), req.getSpecifiers()));
		sendAnswer(requestMessage, answer);
	}

	@MessageHandler(ContentType.FIND_REQ)
	private void handleFindRequest(Message requestMessage) {
		// TODO
	}

	private void sendAnswer(Message req, Content answer) {
		Message ans = msgBuilder.newResponseMessage(req.getHeader(), answer);
		router.sendMessage(ans);
	}

	public Map<KindKey, StoreKindData> getLocalResources() {
		return localStore.getStoredResources();
	}

	public void removeLocalResource(ResourceID resourceId) {
		localStore.removeResource(resourceId);
	}

	public int getSize() {
		return localStore.getSize();
	}
}
