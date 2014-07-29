package com.github.reload.net.encoders;

import java.math.BigInteger;
import java.util.ArrayList;
import org.junit.Assert;
import org.junit.Test;
import com.github.reload.Components;
import com.github.reload.net.encoders.content.AppAttachMessage;
import com.github.reload.net.encoders.content.AttachMessage;
import com.github.reload.net.encoders.content.Content;
import com.github.reload.net.encoders.content.Error;
import com.github.reload.net.encoders.content.Error.ErrorType;
import com.github.reload.net.encoders.content.JoinAnswer;
import com.github.reload.net.encoders.content.JoinRequest;
import com.github.reload.net.encoders.content.LeaveAnswer;
import com.github.reload.net.encoders.content.LeaveRequest;
import com.github.reload.net.encoders.content.PingAnswer;
import com.github.reload.net.encoders.content.PingRequest;
import com.github.reload.net.encoders.content.ProbeAnswer;
import com.github.reload.net.encoders.content.ProbeInformation;
import com.github.reload.net.encoders.content.ProbeRequest;
import com.github.reload.net.encoders.content.ProbeRequest.ProbeInformationType;
import com.github.reload.net.encoders.content.RouteQueryAnswer;
import com.github.reload.net.encoders.content.RouteQueryRequest;
import com.github.reload.net.encoders.content.UpdateAnswer;
import com.github.reload.net.encoders.content.UpdateRequest;
import com.github.reload.net.encoders.header.DestinationList;
import com.github.reload.net.encoders.header.ResourceID;

public class MessageContentTest extends MessageTest {

	@SuppressWarnings("unchecked")
	protected <T extends Content> T sendContent(T content) throws Exception {
		MessageBuilder b = (MessageBuilder) Components.get(MessageBuilder.COMPNAME);

		Message message = b.newMessage(content, new DestinationList(TEST_NODEID));

		Message echo = sendMessage(message);

		return (T) echo.getContent();
	}

	@Test
	public void testAttachRequest() throws Exception {
		AttachMessage.Builder b = new AttachMessage.Builder();
		AttachMessage content = b.buildRequest();
		AttachMessage echo = sendContent(content);

		Assert.assertTrue(echo.isRequest());
		Assert.assertEquals(content.getCandidates(), echo.getCandidates());
		Assert.assertEquals(content.isSendUpdate(), echo.isSendUpdate());
		Assert.assertArrayEquals(content.getPassword(), echo.getPassword());
		Assert.assertArrayEquals(content.getUserFragment(), echo.getUserFragment());
	}

	@Test
	public void testAttachAnswer() throws Exception {
		AttachMessage.Builder b = new AttachMessage.Builder();
		AttachMessage content = b.buildAnswer();
		AttachMessage echo = sendContent(content);

		Assert.assertTrue(echo.isAnswer());
		Assert.assertEquals(content.getCandidates(), echo.getCandidates());
		Assert.assertEquals(content.isSendUpdate(), echo.isSendUpdate());
		Assert.assertArrayEquals(content.getPassword(), echo.getPassword());
		Assert.assertArrayEquals(content.getUserFragment(), echo.getUserFragment());
	}

	@Test
	public void testAppAttachRequest() throws Exception {
		AppAttachMessage.Builder b = new AppAttachMessage.Builder(5060);
		AppAttachMessage content = b.buildRequest();
		AppAttachMessage echo = sendContent(content);

		Assert.assertTrue(echo.isRequest());
		Assert.assertTrue(echo.isActive() == false);
		Assert.assertEquals(content.getCandidates(), echo.getCandidates());
		Assert.assertEquals(content.getApplicationID(), echo.getApplicationID());
		Assert.assertArrayEquals(content.getPassword(), echo.getPassword());
		Assert.assertArrayEquals(content.getUserFragment(), echo.getUserFragment());
	}

	@Test
	public void testAppAttachAnswer() throws Exception {
		AppAttachMessage.Builder b = new AppAttachMessage.Builder(5060);
		AppAttachMessage content = b.buildAnswer();
		AppAttachMessage echo = sendContent(content);

		Assert.assertTrue(echo.isAnswer());
		Assert.assertTrue(echo.isActive() == true);
		Assert.assertEquals(content.getCandidates(), echo.getCandidates());
		Assert.assertEquals(content.getApplicationID(), echo.getApplicationID());
		Assert.assertArrayEquals(content.getPassword(), echo.getPassword());
		Assert.assertArrayEquals(content.getUserFragment(), echo.getUserFragment());
	}

	@Test
	public void testJoinRequest() throws Exception {
		JoinRequest content = new JoinRequest(TEST_NODEID, "JOINRQDATA".getBytes());
		JoinRequest echo = sendContent(content);

		Assert.assertEquals(content.getJoiningNode(), echo.getJoiningNode());
		Assert.assertArrayEquals(content.getOverlayData(), echo.getOverlayData());
	}

	@Test
	public void testJoinAnswer() throws Exception {
		JoinAnswer content = new JoinAnswer("JOINANDDATA".getBytes());
		JoinAnswer echo = sendContent(content);

		Assert.assertArrayEquals(content.getOverlayData(), echo.getOverlayData());
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
		Assert.assertArrayEquals(content.getOverlayData(), echo.getOverlayData());
		Assert.assertEquals(content.isSendUpdate(), echo.isSendUpdate());
	}

	@Test
	public void testRouteQueryAnswer() throws Exception {
		RouteQueryAnswer content = new RouteQueryAnswer("RQRYANS".getBytes());
		RouteQueryAnswer echo = sendContent(content);

		Assert.assertArrayEquals(content.getOverlayData(), echo.getOverlayData());
	}

	@Test
	public void testUpdateRequest() throws Exception {
		UpdateRequest content = new UpdateRequest("UPDATEREQ".getBytes());
		UpdateRequest echo = sendContent(content);

		Assert.assertArrayEquals(content.getOverlayData(), echo.getOverlayData());
	}

	@Test
	public void testUpdateAnswer() throws Exception {
		UpdateAnswer content = new UpdateAnswer("UPDATEANS".getBytes());
		UpdateAnswer echo = sendContent(content);

		Assert.assertArrayEquals(content.getOverlayData(), echo.getOverlayData());
	}

	@Test
	public void testLeaveRequest() throws Exception {
		LeaveRequest content = new LeaveRequest(TEST_NODEID, "LEAVEREQ".getBytes());
		LeaveRequest echo = sendContent(content);

		Assert.assertEquals(content.getLeavingNode(), echo.getLeavingNode());
		Assert.assertArrayEquals(content.getOverlayData(), echo.getOverlayData());
	}

	@Test
	public void testLeaveAnswer() throws Exception {
		LeaveAnswer content = new LeaveAnswer();
		LeaveAnswer echo = sendContent(content);

		Assert.assertEquals(content.isAnswer(), echo.isAnswer());
	}

	@Test
	public void testError() throws Exception {
		Error content = new Error(ErrorType.NOT_FOUND, "Test error message");

		Error echo = sendContent(content);

		Assert.assertEquals(content.getErrorType(), echo.getErrorType());
		Assert.assertEquals(content.getInfo(), echo.getInfo());
	}

}