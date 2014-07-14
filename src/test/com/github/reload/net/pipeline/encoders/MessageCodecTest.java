package com.github.reload.net.pipeline.encoders;

import io.netty.channel.ChannelHandler;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import org.junit.Test;
import com.github.reload.Configuration;
import com.github.reload.message.Content;
import com.github.reload.message.GenericCertificate;
import com.github.reload.message.Header;
import com.github.reload.message.SecurityBlock;
import com.github.reload.message.Signature;
import com.github.reload.message.content.PingRequest;
import com.github.reload.net.NetworkTest;
import com.github.reload.net.pipeline.handlers.ForwardingHandler;
import com.github.reload.net.pipeline.handlers.SRLinkHandler;

public class MessageCodecTest extends NetworkTest {

	@Test
	public void testCodec() throws Exception {
		List<ChannelHandler> codecs = new LinkedList<ChannelHandler>();

		Configuration conf = new Configuration();

		codecs.add(new FramedMessageCodec());
		codecs.add(new SRLinkHandler());
		codecs.add(new HeadedMessageDecoder(conf));
		codecs.add(new ForwardingHandler());
		codecs.add(new MessageEncoder(conf));
		codecs.add(new PayloadDecoder(conf));

		MsgTester tester = initTester(codecs);

		Header h = new Header();
		Content content = new PingRequest(75);
		SecurityBlock s = new SecurityBlock(new ArrayList<GenericCertificate>(), Signature.EMPTY_SIGNATURE);

		Message message = new Message(h, content, s);

		Message ans = (Message) tester.sendMessage(message);

		System.out.println(message.content);
		System.out.println(ans.content);

		tester.shutdown();
	}
}