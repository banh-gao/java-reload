package com.github.reload.storage;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import com.github.reload.APITest;
import com.github.reload.TestConfiguration;
import com.github.reload.TestService;
import com.github.reload.net.encoders.header.ResourceID;
import com.github.reload.services.storage.PreparedData;
import com.github.reload.services.storage.StorageService;
import com.github.reload.services.storage.encoders.ArrayModel.ArrayValueBuilder;
import com.github.reload.services.storage.encoders.ArrayModel.ArrayValueSpecifier;
import com.github.reload.services.storage.encoders.ArrayValue;
import com.github.reload.services.storage.encoders.DictionaryModel.DictionaryValueBuilder;
import com.github.reload.services.storage.encoders.DictionaryModel.DictionaryValueSpecifier;
import com.github.reload.services.storage.encoders.DictionaryValue;
import com.github.reload.services.storage.encoders.FetchKindResponse;
import com.github.reload.services.storage.encoders.SingleModel.SingleValueBuilder;
import com.github.reload.services.storage.encoders.SingleValue;
import com.github.reload.services.storage.encoders.StoreKindDataSpecifier;
import com.github.reload.services.storage.encoders.StoreKindResponse;
import com.github.reload.services.storage.policies.UserMatch.UserParamsGenerator;
import com.google.common.util.concurrent.ListenableFuture;

public class StorageTest extends APITest {

	private static StorageService storServ;
	private final byte[] TEST_SINGLE = "SINGLE_VALUE".getBytes();
	private final byte[] TEST_KEY = "TEST_KEY".getBytes();
	private static int TEST_INDEX = 3;

	@Before
	public void start() throws Exception {
		storServ = overlay.getService(StorageService.SERVICE_ID);
	}

	@Test
	public void testStoreSingle() throws Exception {
		PreparedData p = storServ.newPreparedData(TestConfiguration.TEST_KIND_SINGLE);
		SingleValueBuilder b = (SingleValueBuilder) p.getValueBuilder();
		b.value(TEST_SINGLE);

		UserParamsGenerator pGen = (UserParamsGenerator) storServ.newParamsGenerator(TestConfiguration.TEST_KIND_SINGLE);

		ResourceID STORE_RESID = pGen.getResourceId();

		ListenableFuture<List<StoreKindResponse>> storeFut = storServ.store(STORE_RESID, p);
		storeFut.get();

		StoreKindDataSpecifier spec = storServ.newDataSpecifier(TestConfiguration.TEST_KIND_SINGLE);
		ListenableFuture<List<FetchKindResponse>> fetchFut = storServ.fetch(STORE_RESID, spec);

		SingleValue ans = (SingleValue) fetchFut.get().get(0).getValues().get(0).getValue();

		assertArrayEquals(TEST_SINGLE, ans.getValue());
	}

	@Test
	public void testStoreArray() throws Exception {
		PreparedData p = storServ.newPreparedData(TestConfiguration.TEST_KIND_ARRAY);
		ArrayValueBuilder b = (ArrayValueBuilder) p.getValueBuilder();
		b.index(TEST_INDEX);
		b.value(TEST_SINGLE, true);

		UserParamsGenerator pGen = (UserParamsGenerator) storServ.newParamsGenerator(TestConfiguration.TEST_KIND_SINGLE);
		ResourceID STORE_RESID = pGen.getResourceId();

		ListenableFuture<List<StoreKindResponse>> storeFut = storServ.store(STORE_RESID, p);
		storeFut.get();

		StoreKindDataSpecifier spec = storServ.newDataSpecifier(TestConfiguration.TEST_KIND_ARRAY);
		ArrayValueSpecifier mSpec = (ArrayValueSpecifier) spec.getValueSpecifier();
		mSpec.addRange(3, 4);
		ListenableFuture<List<FetchKindResponse>> fetchFut = storServ.fetch(STORE_RESID, spec);

		ArrayValue ans = (ArrayValue) fetchFut.get().get(0).getValues().get(0).getValue();

		assertTrue(ans.getValue().exists());
		assertArrayEquals(TEST_SINGLE, ans.getValue().getValue());
		assertEquals(TEST_INDEX, ans.getIndex());
	}

	@Test
	public void testStoreDict() throws Exception {
		PreparedData p = storServ.newPreparedData(TestConfiguration.TEST_KIND_DICT);
		DictionaryValueBuilder b = (DictionaryValueBuilder) p.getValueBuilder();
		b.key(TEST_KEY);
		b.value(TEST_SINGLE, true);

		UserParamsGenerator pGen = (UserParamsGenerator) storServ.newParamsGenerator(TestConfiguration.TEST_KIND_DICT);
		ResourceID STORE_RESID = pGen.getResourceId();

		ListenableFuture<List<StoreKindResponse>> storeFut = storServ.store(STORE_RESID, p);
		storeFut.get();

		StoreKindDataSpecifier spec = storServ.newDataSpecifier(TestConfiguration.TEST_KIND_DICT);
		DictionaryValueSpecifier mSpec = (DictionaryValueSpecifier) spec.getValueSpecifier();
		mSpec.addKey(TEST_KEY);
		ListenableFuture<List<FetchKindResponse>> fetchFut = storServ.fetch(STORE_RESID, spec);

		DictionaryValue ans = (DictionaryValue) fetchFut.get().get(0).getValues().get(0).getValue();

		assertTrue(ans.getValue().exists());
		assertArrayEquals(TEST_SINGLE, ans.getValue().getValue());
		assertArrayEquals(TEST_KEY, ans.getKey());
	}

	@Test
	public void testRemove() throws Exception {
		UserParamsGenerator pGen = (UserParamsGenerator) storServ.newParamsGenerator(TestConfiguration.TEST_KIND_SINGLE);
		ResourceID STORE_RESID = pGen.getResourceId();
		PreparedData p = storServ.newPreparedData(TestConfiguration.TEST_KIND_SINGLE);
		ListenableFuture<List<StoreKindResponse>> storeFut = storServ.store(STORE_RESID, p);
		storeFut.get();

		StoreKindDataSpecifier spec = storServ.newDataSpecifier(TestConfiguration.TEST_KIND_SINGLE);
		ListenableFuture<List<FetchKindResponse>> fetchFut = storServ.fetch(STORE_RESID, spec);

		storServ.removeData(STORE_RESID, spec).get();

		fetchFut = storServ.fetch(STORE_RESID, spec);

		// Check returned value is "non-existent" value
		assertEquals(TestConfiguration.TEST_KIND_SINGLE.getDataModel().getNonExistentValue(), fetchFut.get().get(0).getValues().get(0).getValue());
	}

	@Test
	public void testStat() throws Exception {
		TestService s = overlay.getService(TestService.SERVICE_ID);
	}
}
