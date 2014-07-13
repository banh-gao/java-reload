package com.github.reload.storage;

import java.security.GeneralSecurityException;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.github.reload.Configuration;
import com.github.reload.Context.Component;
import com.github.reload.Context.CtxComponent;
import com.github.reload.DataKind;
import com.github.reload.message.ContentType;
import com.github.reload.message.DestinationList;
import com.github.reload.message.MessageBuilder;
import com.github.reload.message.ResourceID;
import com.github.reload.message.SignerIdentity.IdentityType;
import com.github.reload.message.errors.NetworkException;
import com.github.reload.net.MessageRouter;
import com.github.reload.net.encoding.Message;
import com.github.reload.routing.TopologyPlugin;
import com.github.reload.storage.data.ArrayModel.ArrayModelSpecifier;
import com.github.reload.storage.data.DictionaryModel.DictionaryModelSpecifier;
import com.github.reload.storage.data.DictionaryValue.Key;
import com.github.reload.storage.data.StoredData;
import com.github.reload.storage.data.StoredDataSpecifier;
import com.github.reload.storage.net.FetchAnswer;
import com.github.reload.storage.net.FetchKindResponse;
import com.github.reload.storage.net.FetchRequest;
import com.github.reload.storage.net.StoreAnswer;
import com.github.reload.storage.net.StoreKindData;
import com.github.reload.storage.net.StoreKindResponse;
import com.github.reload.storage.net.StoreRequest;

/**
 * Helps a peer to send storage requests into the overlay
 * 
 */
public class StorageClientHelper implements Component {

	private static final short REPLICA_NUMBER = 0;

	@CtxComponent
	private Configuration conf;

	@CtxComponent
	private TopologyPlugin topologyPlugin;

	@CtxComponent
	private MessageRouter msgRouter;

	@CtxComponent
	private MessageBuilder msgBuilder;

	@Override
	public void compStart() {
		// TODO Auto-generated method stub
	}

	public List<StoreKindResponse> sendStoreRequest(DestinationList destination, PreparedData... preparedData) {
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

	public List<FetchKindResponse> sendFetchRequest(DestinationList destination, StoredDataSpecifier... specifiers) {
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

	public List<StoreKindResponse> sendRemoveRequest(DestinationList destination, StoredDataSpecifier dataSpecifier) throws StorageException, NetworkException, InterruptedException {
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