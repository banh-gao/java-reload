package com.github.reload.net.encoders;

import static org.junit.Assert.assertEquals;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import com.github.reload.Components;
import com.github.reload.net.MessageRouter;
import com.github.reload.net.encoders.content.Content;
import com.github.reload.net.encoders.content.storage.ArrayModel;
import com.github.reload.net.encoders.content.storage.ArrayModel.ArrayModelSpecifier;
import com.github.reload.net.encoders.content.storage.ArrayModel.ArrayValueBuilder;
import com.github.reload.net.encoders.content.storage.FetchAnswer;
import com.github.reload.net.encoders.content.storage.FetchKindResponse;
import com.github.reload.net.encoders.content.storage.FetchRequest;
import com.github.reload.net.encoders.content.storage.FindAnswer;
import com.github.reload.net.encoders.content.storage.FindKindData;
import com.github.reload.net.encoders.content.storage.FindRequest;
import com.github.reload.net.encoders.content.storage.SingleValue;
import com.github.reload.net.encoders.content.storage.StatAnswer;
import com.github.reload.net.encoders.content.storage.StatKindResponse;
import com.github.reload.net.encoders.content.storage.StatRequest;
import com.github.reload.net.encoders.content.storage.StoreAnswer;
import com.github.reload.net.encoders.content.storage.StoreKindData;
import com.github.reload.net.encoders.content.storage.StoreKindResponse;
import com.github.reload.net.encoders.content.storage.StoreRequest;
import com.github.reload.net.encoders.content.storage.StoredData;
import com.github.reload.net.encoders.content.storage.StoredDataSpecifier;
import com.github.reload.net.encoders.content.storage.StoredMetadata;
import com.github.reload.net.encoders.header.DestinationList;
import com.github.reload.net.encoders.header.ResourceID;
import com.github.reload.net.encoders.secBlock.HashAlgorithm;
import com.github.reload.net.encoders.secBlock.Signature;
import com.github.reload.storage.AccessPolicy;
import com.github.reload.storage.DataKind;
import com.github.reload.storage.DataKind.Builder;
import com.github.reload.storage.DataModel;
import com.github.reload.storage.policies.NodeMatch;

public class MessageStorageTest extends MessageTest {

	private static final ResourceID TEST_RES = ResourceID.valueOf(new byte[]{
																				1,
																				2,
																				3,
																				4});

	private static final long TEST_KINDID = 25;

	static {
		Builder b = new DataKind.Builder(TEST_KINDID);
		b.accessPolicy(AccessPolicy.getInstance(NodeMatch.class));
		b.dataModel(DataModel.getInstance(DataModel.ARRAY));
		b.attribute(DataKind.MAX_COUNT, 10).attribute(DataKind.MAX_SIZE, 100);
		DataKind.registerDataKind(b.build());
	}

	private static final short TEST_REPLICA = 2;
	private static final DataKind TEST_KIND = DataKind.getInstance(TEST_KINDID);
	private static final byte[] TEST_VALUE = "TESTVALUE".getBytes();

	@SuppressWarnings("unchecked")
	protected <T extends Content> T sendContent(T content) throws Exception {

		MessageBuilder b = (MessageBuilder) Components.get(MessageBuilder.COMPNAME);

		Message message = b.newMessage(content, new DestinationList(TEST_NODEID));

		Message echo = sendMessage(message);

		return (T) echo.getContent();
	}

	@Test
	public void testStoreReq() throws Exception {
		List<StoredData> storedData = new ArrayList<StoredData>();

		ArrayModel model = (ArrayModel) TEST_KIND.getDataModel();
		ArrayValueBuilder b = model.newValueBuilder();

		b.value(new SingleValue(TEST_VALUE, true));
		b.append(true);

		StoredData data = new StoredData(BigInteger.ONE, 255, b.build(), Signature.EMPTY_SIGNATURE);

		storedData.add(data);

		StoreKindData kd = new StoreKindData(TEST_KIND, BigInteger.TEN, storedData);
		StoreRequest req = new StoreRequest(TEST_RES, TEST_REPLICA, Collections.singletonList(kd));

		StoreRequest echo = sendContent(req);

		assertEquals(req.getResourceId(), echo.getResourceId());
		assertEquals(req.getReplicaNumber(), echo.getReplicaNumber());

		for (int i = 0; i < req.getKindData().size(); i++) {
			StoreKindData reqKD = req.getKindData().get(i);
			StoreKindData echoKD = echo.getKindData().get(i);

			assertEquals(reqKD.getGeneration(), echoKD.getGeneration());
			assertEquals(reqKD.getKind(), echoKD.getKind());
			assertEquals(reqKD.getValues(), echoKD.getValues());
		}
	}

	@Test
	public void testStoreAns() throws Exception {

		StoreKindResponse kr = new StoreKindResponse(TEST_KIND, BigInteger.TEN, Collections.singletonList(TEST_NODEID));
		StoreAnswer req = new StoreAnswer(Collections.singletonList(kr));

		StoreAnswer echo = sendContent(req);

		for (int i = 0; i < req.getResponses().size(); i++) {
			StoreKindResponse reqKR = req.getResponses().get(i);
			StoreKindResponse echoKR = echo.getResponses().get(i);

			assertEquals(reqKR.getGeneration(), echoKR.getGeneration());
			assertEquals(reqKR.getKind(), echoKR.getKind());
			assertEquals(reqKR.getReplicas(), echoKR.getReplicas());
		}
	}

	@Test
	public void testFetchReq() throws Exception {

		ArrayModelSpecifier ms = (ArrayModelSpecifier) TEST_KIND.getDataModel().newSpecifier();
		ms.addRange(10, 13);
		StoredDataSpecifier sds = TEST_KIND.newDataSpecifier(ms);
		sds.setGeneration(BigInteger.TEN);

		FetchRequest req = new FetchRequest(TEST_RES, Collections.singletonList(sds));

		FetchRequest echo = sendContent(req);

		assertEquals(req.getResourceId(), echo.getResourceId());

		for (int i = 0; i < req.getSpecifiers().size(); i++) {
			StoredDataSpecifier reqDS = req.getSpecifiers().get(i);
			StoredDataSpecifier echoDS = echo.getSpecifiers().get(i);

			assertEquals(reqDS.getGeneration(), echoDS.getGeneration());
			assertEquals(reqDS.getKind(), echoDS.getKind());
			assertEquals(reqDS.getModelSpecifier(), echoDS.getModelSpecifier());
		}
	}

	@Test
	public void testFetchAns() throws Exception {
		Components.unregister(MessageRouter.class);
		List<StoredData> storedData = new ArrayList<StoredData>();

		ArrayModel model = (ArrayModel) TEST_KIND.getDataModel();
		ArrayValueBuilder b = model.newValueBuilder();

		b.value(new SingleValue(TEST_VALUE, true));
		b.append(true);

		StoredData data = new StoredData(BigInteger.ONE, 255, b.build(), Signature.EMPTY_SIGNATURE);

		storedData.add(data);

		FetchKindResponse kr = new FetchKindResponse(TEST_KIND, BigInteger.TEN, storedData);
		FetchAnswer req = new FetchAnswer(Collections.singletonList(kr));

		FetchAnswer echo = sendContent(req);

		for (int i = 0; i < req.getResponses().size(); i++) {
			FetchKindResponse reqKR = req.getResponses().get(i);
			FetchKindResponse echoKR = echo.getResponses().get(i);

			assertEquals(reqKR.getGeneration(), echoKR.getGeneration());
			assertEquals(reqKR.getKind(), echoKR.getKind());
			assertEquals(reqKR.getValues(), echoKR.getValues());
		}
	}

	@Test
	public void testStatReq() throws Exception {
		ArrayModelSpecifier ms = (ArrayModelSpecifier) TEST_KIND.getDataModel().newSpecifier();
		ms.addRange(10, 13);
		StoredDataSpecifier sds = TEST_KIND.newDataSpecifier(ms);
		sds.setGeneration(BigInteger.TEN);
		StatRequest req = new StatRequest(TEST_RES, Collections.singletonList(sds));

		StatRequest echo = sendContent(req);

		assertEquals(req.getResourceId(), echo.getResourceId());
		assertEquals(req.getSpecifiers(), echo.getSpecifiers());
	}

	@Test
	public void testStatAns() throws Exception {
		List<StoredMetadata> storedData = new ArrayList<StoredMetadata>();

		ArrayModel model = (ArrayModel) TEST_KIND.getDataModel();
		ArrayValueBuilder b = model.newValueBuilder();

		b.value(new SingleValue(TEST_VALUE, true));
		b.append(true);

		StoredMetadata data = new StoredMetadata(BigInteger.TEN, 255, model.newMetadata(b.build(), HashAlgorithm.SHA1));

		storedData.add(data);

		StatKindResponse kr = new StatKindResponse(TEST_KIND, BigInteger.TEN, storedData);
		StatAnswer req = new StatAnswer(Collections.singletonList(kr));

		StatAnswer echo = sendContent(req);

		assertEquals(req.getResponses(), echo.getResponses());
	}

	@Test
	public void testFindReq() throws Exception {
		FindRequest req = new FindRequest(TEST_RES, Collections.singleton(TEST_KIND));

		FindRequest echo = sendContent(req);

		assertEquals(req.getResourceId(), echo.getResourceId());
		assertEquals(req.getKinds(), echo.getKinds());
	}

	@Test
	public void testFindAns() throws Exception {
		FindAnswer req = new FindAnswer(Collections.singleton(new FindKindData(TEST_KIND, TEST_RES)));

		FindAnswer echo = sendContent(req);

		assertEquals(req.getData(), echo.getData());
	}
}
