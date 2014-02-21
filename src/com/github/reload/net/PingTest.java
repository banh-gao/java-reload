package com.github.reload.net;

import org.junit.Test;
import com.github.reload.message.content.PingRequest;

public class PingTest extends TestCodecs {

	@Test
	public void testRequest() throws Exception {
		testMessage(new PingRequest());
	}
}
