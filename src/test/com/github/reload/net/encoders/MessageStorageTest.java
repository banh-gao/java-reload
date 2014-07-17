package com.github.reload.net.encoders;

import static org.junit.Assert.assertEquals;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import com.github.reload.net.encoders.content.Content;
import com.github.reload.net.encoders.content.storage.ArrayModel;
import com.github.reload.net.encoders.content.storage.ArrayModel.ArrayValueBuilder;
import com.github.reload.net.encoders.content.storage.SingleValue;
import com.github.reload.net.encoders.content.storage.StoreKindData;
import com.github.reload.net.encoders.content.storage.StoreRequest;
import com.github.reload.net.encoders.content.storage.StoredData;
import com.github.reload.net.encoders.header.Header;
import com.github.reload.net.encoders.header.ResourceID;
import com.github.reload.net.encoders.secBlock.GenericCertificate;
import com.github.reload.net.encoders.secBlock.SecurityBlock;
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
		b.maxCount(10).maxSize(100);
		DataKind.registerDataKind(b.build());
	}

	private static final short TEST_REPLICA = 2;
	private static final DataKind TEST_KIND = DataKind.getInstance(TEST_KINDID);
	private static final byte[] TEST_VALUE = "TESTVALUE".getBytes();

	@SuppressWarnings("unchecked")
	protected <T extends Content> T sendContent(T content) throws Exception {
		Header h = new Header.Builder().build();
		SecurityBlock s = new SecurityBlock(new ArrayList<GenericCertificate>(), Signature.EMPTY_SIGNATURE);

		Message message = new Message(h, content, s);
		Message echo = sendMessage(message);

		return (T) echo.getContent();
	}

	@Test
	public void testStore() throws Exception {
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

			for (int j = 0; j < reqKD.getValues().size(); j++) {
				StoredData reqSD = reqKD.getValues().get(j);
				StoredData echoSD = echoKD.getValues().get(j);

				assertEquals(reqSD.getLifeTime(), echoSD.getLifeTime());
				assertEquals(reqSD.getStorageTime(), echoSD.getStorageTime());
				assertEquals(reqSD.getValue(), echoSD.getValue());
			}
		}
	}
}
