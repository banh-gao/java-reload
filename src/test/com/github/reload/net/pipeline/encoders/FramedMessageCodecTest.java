package com.github.reload.net.pipeline.encoders;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import java.util.Collections;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import com.github.reload.net.pipeline.PipelineTester;
import com.github.reload.net.pipeline.encoders.FramedMessage.FramedData;

public class FramedMessageCodecTest extends PipelineTester {

	@Test
	public void testCodec() throws Exception {

		List<? extends ChannelHandler> codecs = Collections.singletonList(new FramedMessageCodec());

		MsgTester tester = initTester(codecs);

		final byte[] testData = "TESTO DI PROVA".getBytes();

		final ByteBuf testPayload = tester.getChannel().alloc().buffer();
		testPayload.clear();
		testPayload.writeBytes(testData);

		FramedData frame = new FramedMessage.FramedData(22, testPayload);

		Object message = tester.sendMessage(frame);

		ByteBuf payloadBuf = ((FramedData) message).getPayload();
		byte[] payload = new byte[payloadBuf.readableBytes()];
		payloadBuf.readBytes(payload);

		Assert.assertArrayEquals(testData, payload);

		tester.shutdown();
	}

}