package com.github.reload.appAttach;

import java.net.InetSocketAddress;
import org.junit.Assert;
import org.junit.Test;
import com.github.reload.APITest;
import com.github.reload.net.encoders.header.DestinationList;
import com.github.reload.services.AppAttachService;
import com.google.common.util.concurrent.ListenableFuture;

public class AppAttachTest extends APITest {

	private static final int TEST_PORT = 6120;

	@Test
	public void testRegister() throws Exception {
		AppAttachService srv = overlay.getService(AppAttachService.SERVICE_ID);

		srv.registerApplicativeServer(TEST_PORT);

		for (int i = 0; i < 100; i++)
			srv.requestApplicationAddress(new DestinationList(TEST_NODEID), TEST_PORT);

		ListenableFuture<InetSocketAddress> addrFut = srv.requestApplicationAddress(new DestinationList(TEST_NODEID), TEST_PORT);

		Assert.assertEquals(TEST_PORT, addrFut.get().getPort());
	}
}
