package com.github.reload.services;

import java.math.BigInteger;
import java.util.Random;
import javax.inject.Inject;
import com.github.reload.components.MessageHandlersManager.MessageHandler;
import com.github.reload.net.MessageRouter;
import com.github.reload.net.encoders.Message;
import com.github.reload.net.encoders.MessageBuilder;
import com.github.reload.net.encoders.content.ContentType;
import com.github.reload.net.encoders.content.PingAnswer;

/**
 * Answers remote peers ping requests
 */
public class PingService {

	@Inject
	MessageBuilder msgBuilder;

	@Inject
	MessageRouter router;

	private final Random rand = new Random();

	@MessageHandler(ContentType.PING_REQ)
	private void handleAppAttachRequest(Message requestMessage) {
		BigInteger respTime = BigInteger.valueOf(System.currentTimeMillis());

		router.sendAnswer(requestMessage.getHeader(), new PingAnswer(rand.nextLong(), respTime));

	}
}