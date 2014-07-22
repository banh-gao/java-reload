package com.github.reload.storage;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import com.github.reload.Components;
import com.github.reload.Configuration;
import com.github.reload.net.encoders.Message;
import com.github.reload.net.encoders.content.Content;
import com.github.reload.net.encoders.content.ContentType;
import com.github.reload.net.encoders.content.storage.FetchAnswer;
import com.github.reload.net.encoders.content.storage.FetchKindResponse;
import com.github.reload.net.encoders.content.storage.FetchRequest;
import com.github.reload.net.encoders.content.storage.FindAnswer;
import com.github.reload.net.encoders.content.storage.FindRequest;
import com.github.reload.net.encoders.content.storage.StatAnswer;
import com.github.reload.net.encoders.content.storage.StatKindResponse;
import com.github.reload.net.encoders.content.storage.StatRequest;
import com.github.reload.net.encoders.content.storage.StoreAnswer;
import com.github.reload.net.encoders.content.storage.StoreKindResponse;
import com.github.reload.net.encoders.content.storage.StoreRequest;
import com.github.reload.net.encoders.header.ResourceID;
import com.github.reload.storage.errors.NotFoundException;

/**
 * Elaborate the storage messages and is responsible for local storage
 * management
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
	private final Configuration conf;

	public StorageController(Configuration conf) {
		if (context == null)
			throw new NullPointerException();

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
		Content content = message.getContent();
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

	@Override
	public void init(Components context) {
		// TODO Auto-generated method stub

	}
}
