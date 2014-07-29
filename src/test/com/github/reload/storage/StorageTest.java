package com.github.reload.storage;

import org.junit.BeforeClass;
import org.junit.Test;
import com.github.reload.APITest;
import com.github.reload.Overlay;
import com.github.reload.net.encoders.header.NodeID;

public class StorageTest extends APITest {

	public static NodeID TEST_NODEID = NodeID.valueOf("f16a536ca4028b661fcb864a075f3871");
	private static Overlay overlay;
	private static StorageService storServ;

	@BeforeClass
	public static void init() throws Exception {
		storServ = overlay.getService(StorageService.SERVICE_ID);
	}

	@Test
	public void testStore() {
		storServ.storeData(destination, preparedData);
	}
}
