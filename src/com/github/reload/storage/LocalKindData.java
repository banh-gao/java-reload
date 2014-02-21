package com.github.reload.storage;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import com.github.reload.message.ResourceID;
import com.github.reload.storage.StorageController.QueryType;
import com.github.reload.storage.data.StoredData;
import com.github.reload.storage.data.StoredMetadata;

/**
 * Contains the values specific to a data kind, the implementations are
 * specialized for the data model used by the data kind
 * 
 * @author Daniel Zozin <zdenial@gmx.com>
 * 
 */
public abstract class LocalKindData {

	private static final Logger logger = Logger.getLogger(ReloadOverlay.class);

	protected static final int VALUES_LENGTH_FIELD = EncUtils.U_INT32;

	private final ScheduledExecutorService dataRemoverExecutor;

	// Reference to parent local kinds object to remove itself when empty
	private final LocalKinds localKinds;

	protected final DataKind kind;

	protected final ResourceID resourceId;

	private BigInteger generationCounter;

	protected LocalKindData(ResourceID resourceId, DataKind kind, BigInteger generationCounter, LocalKinds localKinds) {
		dataRemoverExecutor = localKinds.getDataRemoverExecutor();
		this.localKinds = localKinds;
		this.resourceId = resourceId;
		this.kind = kind;
		this.generationCounter = generationCounter;
	}

	protected abstract void implAdd(StoredData data) throws DataTooLargeException;

	protected abstract void implRemove(StoredData data);

	protected abstract StoredData getStoredObjectFor(StoredData data);

	protected abstract boolean contains(StoredData data);

	public abstract int size();

	/**
	 * Add a new data to the kind by authenticating the data and checking the
	 * validity of the request
	 * 
	 * @throws DataTooOldException
	 * @throws ForbittenException
	 */
	public void add(final StoredData requestData, Context context) throws StorageException {
		StoredData.performKindChecks(resourceId, requestData, kind, context);

		if (!contains(requestData)) {
			implAdd(requestData);
		} else {
			StoredData currentValue = getStoredObjectFor(requestData);
			if (currentValue.getStorageTime().compareTo(requestData.getStorageTime()) > 0)
				throw new DataTooOldException("The storage time of the request is older than currently stored value time");

			// replace the existing value with the new one
			implAdd(requestData);
		}

		dataRemoverExecutor.schedule(new Runnable() {

			@Override
			public void run() {
				implRemove(requestData);
				logger.log(Priority.DEBUG, "Stored data (" + resourceId + ") " + requestData + " removed for expired lifetime of " + requestData.getLifeTime() + " seconds");
				if (size() == 0) {
					localKinds.remove(kind.getKindId());
				}
			}
		}, requestData.getLifeTime(), TimeUnit.SECONDS);
	}

	public DataKind getKind() {
		return kind;
	}

	public BigInteger getGeneration() {
		return generationCounter;
	}

	void setGeneration(BigInteger generation) {
		generationCounter = generation;
	}

	@SuppressWarnings("unchecked")
	<T extends ResponseData> List<T> getMatchingValues(DataSpecifier spec, QueryType queryType) {
		if (!spec.getDataKind().equals(kind))
			return Collections.emptyList();

		// If the generation in the request corresponds to the last value update
		// so we don't need to resend the same content
		if (generationCounter.equals(spec.getGeneration()))
			return Collections.emptyList();

		List<? extends ResponseData> out;

		if (queryType == QueryType.FETCH) {
			out = getMatchingDataValues(spec.getModelSpecifier());
			if (out.size() == 0) {
				List<StoredData> tmp = new ArrayList<StoredData>(1);
				tmp.add(StoredData.getNonExistentData(kind));
				out = tmp;
			}
		} else {
			out = getMatchingMetaDataValues(spec.getModelSpecifier());
		}

		return (List<T>) out;
	}

	public StoreKindData asStoredKindData() {
		StoreKindData s = new StoreKindData(kind, generationCounter);

		for (StoredData d : getDataValues()) {
			s.add(d);
		}

		return s;
	}

	public abstract List<StoredData> getDataValues();

	public abstract List<StoredData> getMatchingDataValues(DataModelSpecifier spec);

	public abstract List<StoredMetadata> getMatchingMetaDataValues(DataModelSpecifier spec);

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((kind == null) ? 0 : kind.hashCode());
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
		LocalKindData other = (LocalKindData) obj;
		if (kind == null) {
			if (other.kind != null)
				return false;
		} else if (!kind.equals(other.kind))
			return false;
		return true;
	}

	/**
	 * Called when data kind configuration changes to eliminate possible values
	 * incompatible with new kind definition
	 * 
	 * @throws StorageException
	 */
	protected abstract void changeDataKind(DataKind updatedKind, Context context);
}
