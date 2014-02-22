package com.github.reload.storage;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import com.github.reload.message.ResourceID;
import com.github.reload.message.errors.ErrorMessageException;
import com.github.reload.storage.net.FetchAnswer;
import com.github.reload.storage.net.FetchKindResponse;
import com.github.reload.storage.net.FetchRequest;
import com.github.reload.storage.net.FindAnswer;
import com.github.reload.storage.net.FindRequest;
import com.github.reload.storage.net.StatAnswer;
import com.github.reload.storage.net.StatKindResponse;
import com.github.reload.storage.net.StatRequest;
import com.github.reload.storage.net.StoreAnswer;
import com.github.reload.storage.net.StoreRequest;
import com.github.reload.storage.net.StoreKindResponse;

/**
 * Elaborate the storage messages and is responsible for local storage
 * management
 * 
 * @author Daniel Zozin <zdenial@gmx.com>
 * 
 */
public class StorageController {

	private static final ContentType[] HANDLED_TYPES = new ContentType[]{
																			ContentType.STORE_REQ,
																			ContentType.FETCH_REQ,
																			ContentType.STAT_REQ,
																			ContentType.FIND_REQ};

	public enum QueryType {
		FETCH, STAT
	}

	private final LocalStore localStore;
	private final Context context;

	public StorageController(Context context) {
		if (context == null)
			throw new NullPointerException();

		this.context = context;
		localStore = new LocalStore(context);
	}

	public boolean isStorageRequest(MessageContent content) {
		return Arrays.asList(HANDLED_TYPES).contains(content.getType());
	}

	/**
	 * Process incoming storage messages
	 * 
	 * @throws StorageException
	 */
	public synchronized Message handleStorageRequest(Message message) throws ErrorMessageException {
		MessageContent content = message.getContent();
		switch (content.getType()) {
			case STORE_REQ :
				return handleStoreRequest(message);
			case FETCH_REQ :
				return handleFetchRequest(message);
			case STAT_REQ :
				return handleStatRequest(message);
			case FIND_REQ :
				return handleFindRequest(message);
			default :
				throw new RuntimeException("Invalid storage message of type " + content.getType());
		}

	}

	private Message handleFetchRequest(Message requestMessage) throws ErrorMessageException {
		FetchRequest req = (FetchRequest) requestMessage.getContent();
		FetchAnswer answer = new FetchAnswer(context, localStore.<FetchKindResponse>query(req.getResourceId(), req.getDataSpecifiers(), QueryType.FETCH));
		return getAnswer(requestMessage, answer);
	}

	private Message handleStatRequest(Message requestMessage) throws ErrorMessageException {
		StatRequest req = (StatRequest) requestMessage.getContent();
		MessageContent answer = new StatAnswer(context, localStore.<StatKindResponse>query(req.getResourceId(), req.getDataSpecifiers(), QueryType.STAT));
		return getAnswer(requestMessage, answer);
	}

	private Message handleFindRequest(Message requestMessage) throws ErrorMessageException {
		FindRequest req = (FindRequest) requestMessage.getContent();
		FindAnswer answer = localStore.find(req);

		return getAnswer(requestMessage, answer);
	}

	private Message handleStoreRequest(Message requestMessage) throws ErrorMessageException {
		StoreRequest req = (StoreRequest) requestMessage.getContent();
		if (req.getReplicaNumber() == 0 && !context.getTopologyPlugin().isThisPeerResponsible(req.getResourceId()))
			throw new NotFoundException("Node not responsible for requested resource");

		if (req.getReplicaNumber() > 0 && !context.getTopologyPlugin().isThisNodeValidReplicaFor(requestMessage))
			throw new NotFoundException("Node not valid replica for requested resource");

		List<StoreKindResponse> response = localStore.store(req.getResourceId(), req.getKindData());
		StoreAnswer answer = new StoreAnswer(response);

		return getAnswer(requestMessage, answer);
	}

	private Message getAnswer(Message requestMessage, MessageContent answerContent) {
		Message responseMsg = context.getMessageBuilder().newResponseMessage(requestMessage.getHeader(), answerContent);
		responseMsg.setCachable(true);
		return responseMsg;
	}

	public Map<ResourceID, LocalKinds> getLocalResources() {
		return localStore.getStoredResources();
	}

	public void removeLocalResource(ResourceID resourceId) {
		localStore.removeResource(resourceId);
	}

	public int getSize() {
		return localStore.getSize();
	}

	/**
	 * @return The stored data size in bytes
	 */
	public BigInteger getUsedMemory() {
		return localStore.getUsedMemory();
	}
}
