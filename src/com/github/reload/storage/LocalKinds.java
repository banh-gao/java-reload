package com.github.reload.storage;

import java.math.BigInteger;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import com.github.reload.message.ResourceID;

/**
 * Contains all the locally stored kinds related to a resource id
 * 
 * @author Daniel Zozin <zdenial@gmx.com>
 * 
 */
public class LocalKinds implements ConfigurationUpdateListener {

	private final Context context;

	// Reference to localstore to remove itself when empty
	private final LocalStore localStore;

	private final ResourceID resourceId;

	private final Map<KindId, LocalKindData> storedKinds = new HashMap<KindId, LocalKindData>();

	LocalKinds(ResourceID resourceId, Context context, LocalStore localStore) {
		this.resourceId = resourceId;
		this.context = context;
		this.localStore = localStore;
	}

	ScheduledExecutorService getDataRemoverExecutor() {
		return localStore.getDataRemoverExecutor();
	}

	public BigInteger add(StoredKindData receivedData) throws StorageException {
		LocalKindData storedKindData = get(receivedData.getKind().getKindId());

		if (storedKindData == null) {
			storedKindData = receivedData.getKind().getDataModel().newLocalKindData(resourceId, receivedData.getKind(), BigInteger.ONE, this);
		}

		validateRequest(storedKindData, receivedData);

		for (StoredData sd : receivedData.getValues()) {
			verify(sd);
			storedKindData.add(sd, context);
		}

		BigInteger newGenCounter;
		if (receivedData.getGeneration().equals(BigInteger.ZERO)) {
			newGenCounter = storedKindData.getGeneration().add(BigInteger.ONE);
		} else {
			newGenCounter = receivedData.getGeneration();
		}

		storedKindData.setGeneration(newGenCounter);

		storedKinds.put(receivedData.getKind().getKindId(), storedKindData);

		return newGenCounter;
	}

	private static void validateRequest(LocalKindData localData, StoredKindData receivedData) throws StorageException {
		BigInteger rcvGen = receivedData.getGeneration();

		if (rcvGen.compareTo(BigInteger.ZERO) > 0 && rcvGen.compareTo(localData.getGeneration()) < 0)
			throw new GenerationTooLowException(localData.getGeneration());

		DataKind kind = localData.getKind();

		if (receivedData.size() > kind.getLongAttribute(DataKind.ATTR_MAX_COUNT))
			throw new DataTooLargeException("The amount of stored elements will exceed the kind maximum allowed amount");
	}

	private void verify(StoredData data) throws StorageException {

		try {
			Certificate storerCert = context.getCryptoHelper().getCertificate(data.getSignature().getIdentity()).getOriginalCertificate();
			data.verify(storerCert, resourceId);
		} catch (SignatureException e) {
			throw new ForbittenException("Invalid data signature");
		} catch (SignerIdentityException e) {
			throw new ForbittenException(e.getMessage());
		}
	}

	public LocalKindData get(KindId kindId) {
		return storedKinds.get(kindId);
	}

	public LocalKindData[] getAll() {
		return storedKinds.values().toArray(new LocalKindData[0]);
	}

	public void remove(KindId kindId) {
		storedKinds.remove(kindId);

		if (storedKinds.size() == 0) {
			localStore.removeResource(resourceId);
		}
	}

	public void remove(StoredKindData data) {
		LocalKindData localKindData = storedKinds.get(data.getKind());
		if (localKindData == null)
			return;

		for (StoredData d : data.getValues()) {
			localKindData.implRemove(d);
		}
	}

	public int size() {
		return storedKinds.size();
	}

	@Override
	public void onConfigurationUpdate(ConfigurationEvent e) {
		switch (e.getType()) {
			case KIND_UPDATED :
				LocalKindData d = storedKinds.get(e.getOldDataKind().getKindId());
				if (d == null)
					return;

				d.changeDataKind(e.getNewDataKind(), context);
				break;
			case KIND_REMOVED :
				storedKinds.remove(e.getRemovedDataKind().getKindId());
				break;
			default :
				break;
		}
	}
}