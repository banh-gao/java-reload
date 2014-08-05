package com.github.reload.storage;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import com.github.reload.APITest;
import com.github.reload.TestConfiguration;
import com.github.reload.net.encoders.header.NodeID;
import com.github.reload.net.encoders.header.ResourceID;
import com.github.reload.services.storage.PreparedData;
import com.github.reload.services.storage.StorageService;
import com.github.reload.services.storage.encoders.FetchKindResponse;
import com.github.reload.services.storage.encoders.SingleModel.SingleValueBuilder;
import com.github.reload.services.storage.encoders.StoreKindResponse;
import com.github.reload.services.storage.encoders.StoredDataSpecifier;
import com.google.common.util.concurrent.ListenableFuture;

public class StorageTest extends APITest {

	public static NodeID TEST_NODEID = NodeID.valueOf("f16a536ca4028b661fcb864a075f3871");
	public static ResourceID TEST_RES = ResourceID.valueOf(TEST_NODEID.getData());
	private static StorageService storServ;

	@Before
	public void start() throws Exception {
		storServ = overlay.getService(StorageService.SERVICE_ID);
	}

	@Test
	public void testStore() throws Exception {
		PreparedData p = storServ.newPreparedData(TestConfiguration.TEST_KIND);
		SingleValueBuilder b = (SingleValueBuilder) p.getValueBuilder();
		ListenableFuture<List<StoreKindResponse>> storeFut = storServ.storeData(TEST_RES, p);
		storeFut.get();

		StoredDataSpecifier spec = storServ.newDataSpecifier(TestConfiguration.TEST_KIND);
		ListenableFuture<List<FetchKindResponse>> fetchFut = storServ.fetchData(TEST_RES, spec);
		System.out.println(fetchFut.get().get(0).getValues());

		storServ.removeData(TEST_RES, spec).get();

		fetchFut = storServ.fetchData(TEST_RES, spec);
		System.out.println(fetchFut.get().get(0).getValues());
	}
}
