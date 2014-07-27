package com.github.reload.appAttach;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import org.apache.log4j.Logger;
import com.github.reload.ApplicationID;
import com.github.reload.Components.Component;
import com.github.reload.Components.MessageHandler;
import com.github.reload.Overlay;
import com.github.reload.net.MessageRouter;
import com.github.reload.net.encoders.Message;
import com.github.reload.net.encoders.MessageBuilder;
import com.github.reload.net.encoders.content.AppAttachMessage;
import com.github.reload.net.encoders.content.ContentType;
import com.github.reload.net.encoders.header.DestinationList;
import com.github.reload.net.encoders.header.Header;
import com.github.reload.net.ice.ICEHelper;
import com.github.reload.net.ice.NoSuitableCandidateException;
import com.github.reload.storage.errors.NotFoundException;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

/**
 * Helper class for app attach handling
 * 
 */
@Component(AppAttachComponent.COMPNAME)
public class AppAttachComponent {

	public static final String COMPNAME = "com.github.reload.appAttach.AppAttachComponent";

	private static final Logger logger = Logger.getLogger(Overlay.class);

	@Component
	private MessageBuilder msgBuilder;

	@Component
	private MessageRouter router;

	@Component
	private ICEHelper iceHelper;

	private final Set<ApplicationID> registeredServers = new ConcurrentSkipListSet<ApplicationID>();

	public boolean registerApplicativeServer(ApplicationID applicationID) {
		logger.info(applicationID + " applicative server registered at " + new InetSocketAddress(applicationID.getId()));
		return registeredServers.add(applicationID);
	}

	public boolean unregisterApplicativeServer(ApplicationID applicationID) {
		if (!registeredServers.remove(applicationID))
			return false;

		logger.info(applicationID + " applicative server unregistered");
		return true;

	}

	@MessageHandler(ContentType.APPATTACH_REQ)
	public Message handleAppAttachRequest(Message requestMessage) throws Exception {
		AppAttachMessage request = (AppAttachMessage) requestMessage.getContent();

		if (!isApplicationRegistered(request.getApplicationID()))
			throw new NotFoundException("Application " + request.getApplicationID() + " not registered");

		if (registeredServers.contains(request.getApplicationID()))
			return getAppAttachAnswer(requestMessage.getHeader(), request.getApplicationID());

		throw new RuntimeException();
	}

	private boolean isApplicationRegistered(ApplicationID applicationID) {
		return registeredServers.contains(applicationID);
	}

	private Message getAppAttachAnswer(Header requestHeader, ApplicationID appID) {
		AppAttachMessage.Builder builder = new AppAttachMessage.Builder(appID);

		Message answer = msgBuilder.newResponseMessage(requestHeader, builder.buildAnswer());

		return answer;
	}

	public ListenableFuture<InetSocketAddress> sendAppAttach(DestinationList destinationList, final ApplicationID applicationId) throws IOException {
		AppAttachMessage.Builder appAttachReqBuilder = new AppAttachMessage.Builder(applicationId);

		Message req = msgBuilder.newMessage(appAttachReqBuilder.buildRequest(), destinationList);

		ListenableFuture<Message> ansFut = router.sendRequestMessage(req);

		final SettableFuture<InetSocketAddress> addrFut = SettableFuture.create();

		Futures.addCallback(ansFut, new FutureCallback<Message>() {

			@Override
			public void onSuccess(Message result) {
				AppAttachMessage answer = (AppAttachMessage) result.getContent();
				try {
					addrFut.set(iceHelper.testAndSelectCandidate(answer.getCandidates()).getSocketAddress());
				} catch (NoSuitableCandidateException e) {
					addrFut.setException(new IOException("No suitable direct connection parameters found"));
				}
			}

			@Override
			public void onFailure(Throwable t) {
				addrFut.setException(t);
			}
		});

		return addrFut;
	}

	public Set<ApplicationID> getRegisteredServers() {
		return Collections.unmodifiableSet(registeredServers);
	}
}
