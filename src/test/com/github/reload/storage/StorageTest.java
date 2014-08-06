package com.github.reload.storage;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.math.BigInteger;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import com.github.reload.APITest;
import com.github.reload.TestConfiguration;
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
import com.github.reload.services.storage.encoders.StoreKindResponse;
import com.github.reload.services.storage.encoders.StoredDataSpecifier;
import com.google.common.util.concurrent.ListenableFuture;

public class StorageTest extends APITest {

	public static ResourceID TEST_RES = ResourceID.valueOf(TEST_NODEID.getData());
	private static StorageService storServ;
	private byte[] TEST_SINGLE = "SINGLE_VALUE".getBytes();
	private byte[] TEST_KEY = "TEST_KEY".getBytes();
	private static BigInteger SINGLE_GEN = BigInteger.ONE;
	private static int TEST_INDEX = 3;
	private static BigInteger ARRAY_GEN = BigInteger.ONE;
	private static BigInteger DICT_GEN = BigInteger.ONE;

	@Before
	public void start() throws Exception {
		storServ = overlay.getService(StorageService.SERVICE_ID);
	}

	@Test
	public void testStoreSingle() throws Exception {
		PreparedData p = storServ.newPreparedData(TestConfiguration.TEST_KIND_SINGLE);
		SingleValueBuilder b = (SingleValueBuilder) p.getValueBuilder();
		p.setGeneration(SINGLE_GEN);
		SINGLE_GEN = SINGLE_GEN.add(BigInteger.ONE);
		b.value(TEST_SINGLE);
		ListenableFuture<List<StoreKindResponse>> storeFut = storServ.storeData(TEST_RES, p);
		storeFut.get();

		StoredDataSpecifier spec = storServ.newDataSpecifier(TestConfiguration.TEST_KIND_SINGLE);
		ListenableFuture<List<FetchKindResponse>> fetchFut = storServ.fetchData(TEST_RES, spec);

		SingleValue ans = (SingleValue) fetchFut.get().get(0).getValues().get(0).getValue();

		assertArrayEquals(TEST_SINGLE, ans.getValue());
	}

	@Test
	public void testStoreArray() throws Exception {
		PreparedData p = storServ.newPreparedData(TestConfiguration.TEST_KIND_ARRAY);
		ArrayValueBuilder b = (ArrayValueBuilder) p.getValueBuilder();
		p.setGeneration(ARRAY_GEN);
		ARRAY_GEN = ARRAY_GEN.add(BigInteger.ONE);
		b.index(TEST_INDEX);
		b.value(TEST_SINGLE, true);
		ListenableFuture<List<StoreKindResponse>> storeFut = storServ.storeData(TEST_RES, p);
		storeFut.get();

		StoredDataSpecifier spec = storServ.newDataSpecifier(TestConfiguration.TEST_KIND_ARRAY);
		ArrayValueSpecifier mSpec = (ArrayValueSpecifier) spec.getModelSpecifier();
		mSpec.addRange(3, 4);
		ListenableFuture<List<FetchKindResponse>> fetchFut = storServ.fetchData(TEST_RES, spec);

		ArrayValue ans = (ArrayValue) fetchFut.get().get(0).getValues().get(0).getValue();

		assertTrue(ans.getValue().exists());
		assertArrayEquals(TEST_SINGLE, ans.getValue().getValue());
		assertEquals(TEST_INDEX, ans.getIndex());
	}

	@Test
	public void testStoreDict() throws Exception {
		PreparedData p = storServ.newPreparedData(TestConfiguration.TEST_KIND_DICT);
		DictionaryValueBuilder b = (DictionaryValueBuilder) p.getValueBuilder();
		p.setGeneration(DICT_GEN);
		DICT_GEN = DICT_GEN.add(BigInteger.ONE);
		b.key(TEST_KEY);
		b.value(TEST_SINGLE, true);
		ListenableFuture<List<StoreKindResponse>> storeFut = storServ.storeData(TEST_RES, p);
		storeFut.get();

		StoredDataSpecifier spec = storServ.newDataSpecifier(TestConfiguration.TEST_KIND_DICT);
		DictionaryValueSpecifier mSpec = (DictionaryValueSpecifier) spec.getModelSpecifier();
		mSpec.addKey(TEST_KEY);
		ListenableFuture<List<FetchKindResponse>> fetchFut = storServ.fetchData(TEST_RES, spec);

		DictionaryValue ans = (DictionaryValue) fetchFut.get().get(0).getValues().get(0).getValue();

		assertTrue(ans.getValue().exists());
		assertArrayEquals(TEST_SINGLE, ans.getValue().getValue());
		assertEquals(TEST_KEY, ans.getKey());
	}

	@Test
	public void testRemove() throws Exception {
		PreparedData p = storServ.newPreparedData(TestConfiguration.TEST_KIND_SINGLE);
		p.setGeneration(SINGLE_GEN);
		SINGLE_GEN = SINGLE_GEN.add(BigInteger.ONE);
		ListenableFuture<List<StoreKindResponse>> storeFut = storServ.storeData(TEST_RES, p);
		storeFut.get();

		StoredDataSpecifier spec = storServ.newDataSpecifier(TestConfiguration.TEST_KIND_SINGLE);
		ListenableFuture<List<FetchKindResponse>> fetchFut = storServ.fetchData(TEST_RES, spec);
		System.out.println(fetchFut.get().get(0).getValues());
		SINGLE_GEN = SINGLE_GEN.add(BigInteger.ONE);
		storServ.removeData(TEST_RES, spec).get();

		fetchFut = storServ.fetchData(TEST_RES, spec);

		// Check returned value is "non-existent" value
		assertEquals(TestConfiguration.TEST_KIND_SINGLE.getDataModel().getNonExistentValue(), fetchFut.get().get(0).getValues().get(0).getValue());
	}
}
