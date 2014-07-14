package com.github.reload.message.content;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import java.math.BigInteger;
import java.net.InetAddress;
import java.util.ArrayList;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import com.github.reload.Configuration;
import com.github.reload.MessageBus;
import com.github.reload.message.Content;
import com.github.reload.message.GenericCertificate;
import com.github.reload.message.Header;
import com.github.reload.message.NodeID;
import com.github.reload.message.ResourceID;
import com.github.reload.message.SecurityBlock;
import com.github.reload.message.Signature;
import com.github.reload.message.content.ProbeRequest.ProbeInformationType;
import com.github.reload.net.NetworkTest;
import com.github.reload.net.ice.IceCandidate.OverlayLinkType;
import com.github.reload.net.pipeline.ReloadStackInitializer;
import com.github.reload.net.pipeline.encoders.Message;
import com.google.common.eventbus.Subscribe;

public class MessageContentTest extends NetworkTest {

	private static Channel ch;
	private static Content answer;

	@BeforeClass
	public static void initPipeline() throws Exception {
		Configuration conf = new Configuration();
		conf.setOverlayAttribute(Configuration.NODE_ID_LENGTH, 5);
		MessageBus messageBus = new MessageBus();
		ReloadStackInitializer initializer = new ReloadStackInitializer(OverlayLinkType.TLS_TCP_FH_NO_ICE, conf, messageBus);

		EventLoopGroup workerGroup = new NioEventLoopGroup();
		Bootstrap b = new Bootstrap();
		b.group(workerGroup);
		b.channel(NioSocketChannel.class);
		b.option(ChannelOption.SO_KEEPALIVE, true);
		b.handler(initializer);

		ch = b.connect(InetAddress.getLocalHost(), TEST_PORT).sync().channel();

		messageBus.register(new TestListener());
	}

	@Test
	public void testAttachRequest() throws Exception {
		AttachMessage.Builder b = new AttachMessage.Builder();
		AttachMessage content = b.buildRequest();
		AttachMessage echo = sendContent(content);

		Assert.assertTrue(echo.isRequest());
		Assert.assertEquals(content.getCandidates(), echo.getCandidates());
		Assert.assertEquals(content.isSendUpdate(), echo.isSendUpdate());
		Assert.assertEquals(content.getPassword(), echo.getPassword());
		Assert.assertEquals(content.getUserFragment(), echo.getUserFragment());
	}

	@Test
	public void testAttachAnswer() throws Exception {
		AttachMessage.Builder b = new AttachMessage.Builder();
		AttachMessage content = b.buildAnswer();
		AttachMessage echo = sendContent(content);

		Assert.assertTrue(echo.isAnswer());
		Assert.assertEquals(content.getCandidates(), echo.getCandidates());
		Assert.assertEquals(content.isSendUpdate(), echo.isSendUpdate());
		Assert.assertEquals(content.getPassword(), echo.getPassword());
		Assert.assertEquals(content.getUserFragment(), echo.getUserFragment());
	}

	@Test
	public void testAppAttachRequest() throws Exception {
		AppAttachMessage.Builder b = new AppAttachMessage.Builder();
		AppAttachMessage content = b.buildRequest();
		AppAttachMessage echo = sendContent(content);

		Assert.assertTrue(echo.isRequest());
		Assert.assertTrue(echo.isActive() == false);
		Assert.assertEquals(content.getCandidates(), echo.getCandidates());
		Assert.assertEquals(content.getApplicationID(), echo.getApplicationID());
		Assert.assertEquals(content.getPassword(), echo.getPassword());
		Assert.assertEquals(content.getUserFragment(), echo.getUserFragment());
	}

	@Test
	public void testAppAttachAnswer() throws Exception {
		AppAttachMessage.Builder b = new AppAttachMessage.Builder();
		AppAttachMessage content = b.buildAnswer();
		AppAttachMessage echo = sendContent(content);

		Assert.assertTrue(echo.isAnswer());
		Assert.assertTrue(echo.isActive() == true);
		Assert.assertEquals(content.getCandidates(), echo.getCandidates());
		Assert.assertEquals(content.getApplicationID(), echo.getApplicationID());
		Assert.assertEquals(content.getPassword(), echo.getPassword());
		Assert.assertEquals(content.getUserFragment(), echo.getUserFragment());
	}

	@Test
	public void testJoinRequest() throws Exception {
		JoinRequest content = new JoinRequest(NodeID.valueOf(new byte[]{1, 2,
																		3, 4}), "JOINREQDATA".getBytes());
		JoinRequest echo = sendContent(content);

		Assert.assertEquals(content.getJoiningNode(), echo.getJoiningNode());
		Assert.assertEquals(content.getOverlayData(), echo.getOverlayData());
	}

	@Test
	public void testJoinAnswer() throws Exception {
		JoinAnswer content = new JoinAnswer("JOINANDDATA".getBytes());
		JoinAnswer echo = sendContent(content);

		Assert.assertEquals(content.getOverlayData(), echo.getOverlayData());
	}

	@Test
	public void testPingRequest() throws Exception {
		PingRequest content = new PingRequest(80);
		PingRequest echo = sendContent(content);

		Assert.assertEquals(content.getPayloadLength(), echo.getPayloadLength());
	}

	@Test
	public void testPingAnswer() throws Exception {
		PingAnswer content = new PingAnswer(555, BigInteger.valueOf(123456789));
		PingAnswer echo = sendContent(content);

		Assert.assertEquals(content.getResponseId(), echo.getResponseId());
		Assert.assertEquals(content.getResponseTime(), echo.getResponseTime());
	}

	@Test
	public void testProbeRequest() throws Exception {
		ProbeRequest content = new ProbeRequest(new ArrayList<ProbeInformationType>());
		ProbeRequest echo = sendContent(content);

		Assert.assertEquals(content.getRequestedInfo(), echo.getRequestedInfo());
	}

	@Test
	public void testProbeAnswer() throws Exception {
		ProbeAnswer content = new ProbeAnswer(new ArrayList<ProbeInformation>());
		ProbeAnswer echo = sendContent(content);

		Assert.assertEquals(content.getProbeInformations(), echo.getProbeInformations());
	}

	@Test
	public void testRouteQueryRequest() throws Exception {
		RouteQueryRequest content = new RouteQueryRequest(ResourceID.valueOf(new byte[]{
																						1,
																						2,
																						3,
																						4}), "RQRYREQ".getBytes(), true);
		RouteQueryRequest echo = sendContent(content);

		Assert.assertEquals(content.getDestination(), echo.getDestination());
		Assert.assertEquals(content.getOverlayData(), echo.getOverlayData());
		Assert.assertEquals(content.isSendUpdate(), echo.isSendUpdate());
	}

	@Test
	public void testRouteQueryAnswer() throws Exception {
		RouteQueryAnswer content = new RouteQueryAnswer("RQRYANS".getBytes());
		RouteQueryAnswer echo = sendContent(content);

		Assert.assertEquals(content.getOverlayData(), echo.getOverlayData());
	}

	@Test
	public void testUpdateRequest() throws Exception {
		UpdateRequest content = new UpdateRequest("UPDATEREQ".getBytes());
		UpdateRequest echo = sendContent(content);

		Assert.assertEquals(content.getOverlayData(), echo.getOverlayData());
	}

	@Test
	public void testUpdateAnswer() throws Exception {
		UpdateAnswer content = new UpdateAnswer("UPDATEANS".getBytes());
		UpdateAnswer echo = sendContent(content);

		Assert.assertEquals(content.getOverlayData(), echo.getOverlayData());
	}

	@Test
	public void testLeaveRequest() throws Exception {
		LeaveRequest content = new LeaveRequest(NodeID.valueOf(new byte[]{1, 2,
																			3,
																			4,}), "LEAVEREQ".getBytes());
		LeaveRequest echo = sendContent(content);

		Assert.assertEquals(content.getLeavingNode(), echo.getLeavingNode());
		Assert.assertEquals(content.getOverlayData(), echo.getOverlayData());
	}

	@Test
	public void testLeaveAnswer() throws Exception {
		LeaveAnswer content = new LeaveAnswer();
		LeaveAnswer echo = sendContent(content);

		Assert.assertEquals(content.isAnswer(), echo.isAnswer());
	}

	@AfterClass
	public static void deinitPipeline() throws InterruptedException {
		ch.close();
	}

	@SuppressWarnings("unchecked")
	protected <T extends Content> T sendContent(T content) throws Exception {
		Header h = new Header();
		SecurityBlock s = new SecurityBlock(new ArrayList<GenericCertificate>(), Signature.EMPTY_SIGNATURE);

		Message message = new Message(h, content, s);
		ch.writeAndFlush(message).sync();

		synchronized (ch) {
			ch.wait();
		}
		return (T) answer;
	}

	static class TestListener {

		@Subscribe
		public void messageReceived(Message message) {
			answer = message.getContent();
			synchronized (ch) {
				ch.notify();
			}
		}
	}
}