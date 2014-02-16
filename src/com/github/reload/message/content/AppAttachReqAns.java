package com.github.reload.message.content;

import java.net.InetSocketAddress;
import java.util.List;
import com.github.reload.message.Content;
import com.github.reload.message.ContentType;
import com.github.reload.net.ice.ICEHelper;
import com.github.reload.net.ice.IceCandidate;

/**
 * common representation of AppAttach requests and answers
 * 
 * @author Daniel Zozin <zdenial@gmx.com>
 * 
 */
public class AppAttachReqAns extends Content {

	byte[] userFragment;
	byte[] password;
	ApplicationID applicationID;
	boolean isActive;
	List<IceCandidate> candidates;

	AppAttachReqAns() {
	}

	private AppAttachReqAns(Builder builder) {
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
	 * @author Daniel Zozin <zdenial@gmx.com>
	 * 
	 */
	public static class Builder {

		boolean isActive;
		byte[] userFragment;
		byte[] password;
		ApplicationID applicationID;
		List<IceCandidate> candidates;

		public Builder(int port, ICEHelper iceHelper) {
			userFragment = iceHelper.getUserFragment();
			password = iceHelper.getPassword();
			candidates = iceHelper.getCandidates(new InetSocketAddress(port));
		}

		public Builder applicationID(ApplicationID applicationID) {
			this.applicationID = applicationID;
			return this;
		}

		public AppAttachReqAns buildRequest() {
			isActive = false;
			return new AppAttachReqAns(this);
		}

		public AppAttachReqAns buildAnswer() {
			isActive = true;
			return new AppAttachReqAns(this);
		}
	}

	@Override
	public ContentType getType() {
		if (isRequest())
			return ContentType.APPATTACH_REQ;
		else
			return ContentType.APPATTACH_ANS;
	}
}
