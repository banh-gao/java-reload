package com.github.reload.storage;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import com.github.reload.APITest;
import com.github.reload.TestConfiguration;
import com.github.reload.net.codecs.header.ResourceID;
import com.github.reload.services.storage.PreparedData;
import com.github.reload.services.storage.StorageService;
import com.github.reload.services.storage.net.ArrayValue;
import com.github.reload.services.storage.net.ArrayValueSpecifier;
import com.github.reload.services.storage.net.DictionaryValue;
import com.github.reload.services.storage.net.DictionaryValueSpecifier;
import com.github.reload.services.storage.net.FetchKindResponse;
import com.github.reload.services.storage.net.SingleValue;
import com.github.reload.services.storage.net.StoreKindResponse;
import com.github.reload.services.storage.net.StoreKindSpecifier;
import com.github.reload.services.storage.policies.UserMatch.UserRIDGenerator;
import com.google.common.util.concurrent.ListenableFuture;

public class StorageTest extends APITest {

	private static StorageService storServ;
	private final byte[] TEST_SINGLE = "SINGLE_VALUE".getBytes();
	private final byte[] TEST_KEY = "TEST_KEY".getBytes();
	private static int TEST_INDEX = 3;

	@Before
	public void start() throws Exception {
		storServ = overlay.getService(StorageService.class);
	}

	@Test
	public void testStoreSingle() throws Exception {
		PreparedData p = storServ.newPreparedData(TestConfiguration.TEST_KIND_SINGLE);
		SingleValue v = (SingleValue) p.getValue();
		v.setValue(TEST_SINGLE);

		UserRIDGenerator pGen = (UserRIDGenerator) storServ.newIDGenerator(TestConfiguration.TEST_KIND_SINGLE);

		ResourceID STORE_RESID = pGen.generateID();

		ListenableFuture<List<StoreKindResponse>> storeFut = storServ.store(STORE_RESID, p);
		storeFut.get();

		StoreKindSpecifier spec = storServ.newDataSpecifier(TestConfiguration.TEST_KIND_SINGLE);
		ListenableFuture<List<FetchKindResponse>> fetchFut = storServ.fetch(STORE_RESID, spec);

		SingleValue ans = (SingleValue) fetchFut.get().get(0).getValues().get(0).getValue();

		assertArrayEquals(TEST_SINGLE, ans.getValue());
	}

	@Test
	public void testStoreArray() throws Exception {
		PreparedData p = storServ.newPreparedData(TestConfiguration.TEST_KIND_ARRAY);
		ArrayValue v = (ArrayValue) p.getValue();
		v.setIndex(TEST_INDEX);
		v.setValue(TEST_SINGLE, true);

		UserRIDGenerator pGen = (UserRIDGenerator) storServ.newIDGenerator(TestConfiguration.TEST_KIND_SINGLE);
		ResourceID STORE_RESID = pGen.generateID();

		ListenableFuture<List<StoreKindResponse>> storeFut = storServ.store(STORE_RESID, p);
		storeFut.get();

		StoreKindSpecifier spec = storServ.newDataSpecifier(TestConfiguration.TEST_KIND_ARRAY);
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
		DictionaryValue v = (DictionaryValue) p.getValue();
		v.setKey(TEST_KEY);
		v.setValue(TEST_SINGLE, true);

		UserRIDGenerator pGen = (UserRIDGenerator) storServ.newIDGenerator(TestConfiguration.TEST_KIND_DICT);
		ResourceID STORE_RESID = pGen.generateID();

		ListenableFuture<List<StoreKindResponse>> storeFut = storServ.store(STORE_RESID, p);
		storeFut.get();

		StoreKindSpecifier spec = storServ.newDataSpecifier(TestConfiguration.TEST_KIND_DICT);
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
		UserRIDGenerator pGen = (UserRIDGenerator) storServ.newIDGenerator(TestConfiguration.TEST_KIND_SINGLE);
		ResourceID STORE_RESID = pGen.generateID();
		PreparedData p = storServ.newPreparedData(TestConfiguration.TEST_KIND_SINGLE);
		ListenableFuture<List<StoreKindResponse>> storeFut = storServ.store(STORE_RESID, p);
		storeFut.get();

		StoreKindSpecifier spec = storServ.newDataSpecifier(TestConfiguration.TEST_KIND_SINGLE);
		ListenableFuture<List<FetchKindResponse>> fetchFut = storServ.fetch(STORE_RESID, spec);

		storServ.removeData(STORE_RESID, spec).get();

		fetchFut = storServ.fetch(STORE_RESID, spec);

		// Check returned value is "non-existent" value
		assertEquals(TestConfiguration.TEST_KIND_SINGLE.getDataModel().getNonExistentValue(), fetchFut.get().get(0).getValues().get(0).getValue());
	}
}
