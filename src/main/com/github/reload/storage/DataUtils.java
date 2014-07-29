package com.github.reload.storage;

import java.math.BigInteger;
import com.github.reload.conf.Configuration;
import com.github.reload.net.encoders.content.Error;
import com.github.reload.net.encoders.content.Error.ErrorMessageException;
import com.github.reload.net.encoders.content.Error.ErrorType;
import com.github.reload.net.encoders.header.ResourceID;
import com.github.reload.net.encoders.secBlock.Signature;
import com.github.reload.storage.AccessPolicy.AccessPolicyException;
import com.github.reload.storage.DataModel.DataValueBuilder;
import com.github.reload.storage.PreparedData.DataBuildingException;
import com.github.reload.storage.encoders.StoredData;

public class DataUtils {

	/**
	 * Check if the stored data object is valid for the specified data kind
	 * 
	 * @throws AccessPolicyException
	 * @throws DataTooLargeException
	 */
	static void performKindChecks(ResourceID resourceId, StoredData requestData, DataKind kind, Configuration conf) throws ErrorMessageException {
		kind.getAccessPolicy().accept(resourceId, requestData, requestData.getSignature().getIdentity());

		if (requestData.getValue().getSize() > kind.getAttribute(DataKind.MAX_SIZE))
			throw new ErrorMessageException(new Error(ErrorType.DATA_TOO_LARGE, "Size of the data exceeds the maximum allowed size"));
	}

	/**
	 * Syntetic data value used to indicate non existent data in particular
	 * situations
	 */
	static StoredData getNonExistentData(DataKind kind) {
		try {
			DataValueBuilder<?> svb = kind.getDataModel().newValueBuilder();
			svb.build();
			return new StoredData(BigInteger.ZERO, 0, svb.build(), Signature.EMPTY_SIGNATURE);
		} catch (DataBuildingException e) {
			throw new RuntimeException(e);
		}
	}
}
