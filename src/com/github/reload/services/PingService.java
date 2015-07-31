package com.github.reload.services;

import java.math.BigInteger;
import java.util.Random;
import javax.inject.Inject;
import com.github.reload.Service;
import com.github.reload.net.MessageRouter;
import com.github.reload.net.codecs.Message;
import com.github.reload.net.codecs.MessageBuilder;
import com.github.reload.net.codecs.content.ContentType;
import com.github.reload.net.codecs.content.PingAnswer;
import com.github.reload.routing.MessageHandlers;
import com.github.reload.routing.MessageHandlers.MessageHandler;
import com.github.reload.services.PingService.PingModule;
import dagger.Module;

/**
 * Answers remote peers ping requests
 */
@Service({PingModule.class})
public class PingService {

	@Inject
	MessageBuilder msgBuilder;

	@Inject
	MessageRouter router;

	private final Random rand = new Random();

	@Inject
	public PingService(MessageHandlers msgHandlers) {
		msgHandlers.register(this);
	}

	@MessageHandler(ContentType.PING_REQ)
	private void handlePingRequest(Message requestMessage) {
		BigInteger respTime = BigInteger.valueOf(System.currentTimeMillis());

		router.sendAnswer(requestMessage.getHeader(), new PingAnswer(rand.nextLong(), respTime));

	}

	@Module(injects = {PingService.class}, complete = false)
	public static class PingModule {

	}
}