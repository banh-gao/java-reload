package com.github.reload.net.encoders;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import java.util.Collections;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import com.github.reload.net.NetworkTest;
import com.github.reload.net.encoders.FramedMessage;
import com.github.reload.net.encoders.FramedMessageCodec;
import com.github.reload.net.encoders.FramedMessage.FramedData;

public class FramedMessageCodecTest extends NetworkTest {

	@Test
	public void testCodec() throws Exception {

		List<? extends ChannelHandler> codecs = Collections.singletonList(new FramedMessageCodec());

		MsgTester tester = initTester(codecs);

		final byte[] testData = "TESTPAYLOAD".getBytes();

		final ByteBuf testPayload = tester.getChannel().alloc().buffer();
		testPayload.clear();
		testPayload.writeBytes(testData);

		FramedData frame = new FramedMessage.FramedData(22, testPayload);

		Object message = tester.sendMessage(frame);

		ByteBuf payloadBuf = ((FramedData) message).getPayload();
		byte[] payload = new byte[payloadBuf.readableBytes()];
		payloadBuf.readBytes(payload);

		Assert.assertArrayEquals(testData, payload);
		Assert.assertEquals(frame.getSequence(), ((FramedData) message).getSequence());

		tester.shutdown();
	}

}