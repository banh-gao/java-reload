package com.github.reload.message.content;

import java.util.ArrayList;
import java.util.List;
import com.github.reload.ApplicationID;
import com.github.reload.message.Codec.ReloadCodec;
import com.github.reload.message.Content;
import com.github.reload.message.ContentType;
import com.github.reload.net.ice.IceCandidate;

@ReloadCodec(AppAttachMessageCodec.class)
public class AppAttachMessage extends Content {

	private byte[] userFragment;
	private byte[] password;
	private ApplicationID applicationID;
	private boolean isActive;
	private List<IceCandidate> candidates;

	AppAttachMessage() {
	}

	private AppAttachMessage(Builder builder) {
		userFragment = builder.userFragment;
		password = builder.password;
		applicationID = builder.applicationID;
		isActive = builder.isActive;
		candidates = builder.candidates;
	}

	/**
	 * @return The ICE candidiates proposed by the sender
	 */
	public List<IceCandidate> getCandidates() {
		return candidates;
	}

	/**
	 * @return The ICE password
	 */
	public byte[] getPassword() {
		return password;
	}

	/**
	 * @return The ICE role
	 */
	public boolean isActive() {
		return isActive;
	}

	/**
	 * @return The ICE user fragment
	 */
	public byte[] getUserFragment() {
		return userFragment;
	}

	/**
	 * @return The identifier of the application that will use the established
	 *         connection
	 */
	public ApplicationID getApplicationID() {
		return applicationID;
	}

	/**
	 * Builder for AppAttach requests and answers
	 * 
	 */
	public static class Builder {

		byte[] userFragment = new byte[0];
		byte[] password = new byte[0];
		List<IceCandidate> candidates = new ArrayList<IceCandidate>();
		boolean sendUpdate = false;
		boolean isActive;
		ApplicationID applicationID;

		public Builder applicationID(ApplicationID applicationID) {
			this.applicationID = applicationID;
			return this;
		}

		public AppAttachMessage buildRequest() {
			isActive = false;
			return new AppAttachMessage(this);
		}

		public AppAttachMessage buildAnswer() {
			isActive = true;
			return new AppAttachMessage(this);
		}

		public Builder candidates(List<IceCandidate> candidates) {
			this.candidates = candidates;
			return this;
		}

		public Builder password(byte[] password) {
			this.password = password;
			return this;
		}

		public Builder userFragment(byte[] userFragment) {
			this.userFragment = userFragment;
			return this;
		}

		public Builder sendUpdate(boolean sendUpdate) {
			this.sendUpdate = sendUpdate;
			return this;
		}
	}

	@Override
	public ContentType getType() {
		if (isActive)
			return ContentType.APPATTACH_ANS;
		else
			return ContentType.APPATTACH_REQ;
	}
}
