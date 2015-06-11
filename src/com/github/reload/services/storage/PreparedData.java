package com.github.reload.services.storage;

import java.math.BigInteger;
import java.util.Date;
import com.github.reload.components.ComponentsContext;
import com.github.reload.crypto.CryptoHelper;
import com.github.reload.crypto.Signer;
import com.github.reload.net.encoders.header.ResourceID;
import com.github.reload.services.storage.encoders.StoredData;
import com.github.reload.services.storage.encoders.DataModel.DataValue;
import com.github.reload.services.storage.encoders.DataModel.DataValueBuilder;

/**
 * Helps to generate a signed data
 * 
 */
public class PreparedData {

	private final static int DEFAULT_LIFETIME = 60;

	public static final long MAX_LIFETIME = 0xffffffffl;
	public static final BigInteger MAX_GENERATION = new BigInteger(1, new byte[]{
																					(byte) 0xff,
																					(byte) 0xff,
																					(byte) 0xff,
																					(byte) 0xff,
																					(byte) 0xff,
																					(byte) 0xff,
																					(byte) 0xff,
																					(byte) 0xff});

	private final DataKind kind;
	private final DataValueBuilder<?> valueBuilder;

	BigInteger generation = BigInteger.ONE;
	BigInteger storageTime = BigInteger.valueOf(new Date().getTime());
	long lifeTime = DEFAULT_LIFETIME;

	PreparedData(DataKind kind) {
		this.kind = kind;
		this.valueBuilder = kind.getDataModel().newValueBuilder();
	}

	public DataKind getKind() {
		return kind;
	}

	/**
	 * Set the date this data is stored, this is used in store checks for future
	 * stores on the same resource-id. If not specified, the current system time
	 * will be used.
	 */
	public PreparedData setStorageTime(Date storageTime) {
		this.storageTime = BigInteger.valueOf(storageTime.getTime());
		return this;
	}

	/**
	 * Set the number of seconds until this value should be deleted from the
	 * overlay.
	 */
	public PreparedData setLifeTime(long lifeTime) {
		this.lifeTime = lifeTime;
		return this;
	}

	/**
	 * @param generation
	 *            The version number of this data into the overlay storage, if
	 *            the value is less than the currently stored value the store
	 *            request will be rejected
	 */
	public PreparedData setGeneration(BigInteger generation) {
		this.generation = generation;
		return this;
	}

	/**
	 * @return the prepared value backed with this data, the runtime prepared
	 *         value type depends on the data model used by the associated data
	 *         kind
	 */
	public DataValueBuilder<?> getValueBuilder() {
		return valueBuilder;
	}

	/**
	 * Creates the stored data object
	 * 
	 * @param context
	 *            The connection context in which the data will be used
	 * @param resourceId
	 *            The resource where the data will be stored. This is needed for
	 *            data signature computation
	 * 
	 * @return The stored data for the specified overlay
	 * @throws DataBuildingException
	 */
	public StoredData build(ComponentsContext ctx, ResourceID resId) {
		DataValue value = valueBuilder.build();

		Signer dataSigner = ctx.get(CryptoHelper.class).newSigner();

		return new StoredData(storageTime, lifeTime, value, dataSigner, resId, kind);
	}
}