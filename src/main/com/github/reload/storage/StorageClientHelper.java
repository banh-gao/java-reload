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
import com.github.reload.Components.Component;
import com.github.reload.Components.CtxComponent;
import com.github.reload.conf.Configuration;
import com.github.reload.net.MessageRouter;
import com.github.reload.net.encoders.MessageBuilder;
import com.github.reload.net.encoders.content.ContentType;
import com.github.reload.net.encoders.content.errors.NetworkException;
import com.github.reload.net.encoders.content.storage.FetchAnswer;
import com.github.reload.net.encoders.content.storage.FetchKindResponse;
import com.github.reload.net.encoders.content.storage.FetchRequest;
import com.github.reload.net.encoders.content.storage.StoreAnswer;
import com.github.reload.net.encoders.content.storage.StoreKindData;
import com.github.reload.net.encoders.content.storage.StoreKindResponse;
import com.github.reload.net.encoders.content.storage.StoreRequest;
import com.github.reload.net.encoders.content.storage.StoredData;
import com.github.reload.net.encoders.content.storage.StoredDataSpecifier;
import com.github.reload.net.encoders.content.storage.ArrayModel.ArrayModelSpecifier;
import com.github.reload.net.encoders.content.storage.DictionaryModel.DictionaryModelSpecifier;
import com.github.reload.net.encoders.content.storage.DictionaryValue.Key;
import com.github.reload.net.encoders.header.DestinationList;
import com.github.reload.net.encoders.header.ResourceID;
import com.github.reload.net.encoders.secBlock.SignerIdentity.IdentityType;
import com.github.reload.net.encoding.Message;
import com.github.reload.routing.TopologyPlugin;

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
