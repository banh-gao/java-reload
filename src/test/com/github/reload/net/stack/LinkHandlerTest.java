package com.github.reload.net.stack;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import java.util.LinkedList;
import java.util.List;
import org.junit.Test;
import com.github.reload.net.NetworkTest;
import com.github.reload.net.encoders.FramedMessage;
import com.github.reload.net.encoders.FramedMessageCodec;
import com.github.reload.net.encoders.FramedMessage.FramedData;
import com.github.reload.net.stack.SRLinkHandler;

public class LinkHandlerTest extends NetworkTest {

	@Test
	public void testSimpleReliability() throws Exception {
		List<ChannelHandler> handlers = new LinkedList<ChannelHandler>();

		handlers.add(new FramedMessageCodec());
		SRLinkHandler linkHandler = new SRLinkHandler();
		handlers.add(linkHandler);

		MsgTester tester = initTester(handlers);

		final byte[] testData = "TESTPAYLOAD".getBytes();

		final ByteBuf testPayload = tester.getChannel().alloc().buffer();
		testPayload.clear();
		testPayload.writeBytes(testData);

		FramedData frame = new FramedMessage.FramedData(22, testPayload);

		tester.sendMessage(frame);

		// TODO:check acknoledgment messages and link handler behaviour

		tester.shutdown();
	}

}