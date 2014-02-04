package com.github.reload.message.content;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import net.sf.jReload.ApplicationID;
import net.sf.jReload.message.ContentType;
import net.sf.jReload.message.DecodingException;
import net.sf.jReload.message.EncUtils;
import net.sf.jReload.message.MessageContent;
import net.sf.jReload.message.UnsignedByteBuffer;
import net.sf.jReload.message.UnsignedByteBuffer.Field;
import net.sf.jReload.net.ice.ICEHelper;
import net.sf.jReload.net.ice.IceCandidate;

/**
 * common representation of AppAttach requests and answers
 * 
 * @author Daniel Zozin <zdenial@gmx.com>
 * 
 */
public class AppAttachReqAns extends MessageContent {

	private final static int UFRAG_LENGTH_FIELD = EncUtils.U_INT8;
	private final static int PASS_LENGTH_FIELD = EncUtils.U_INT8;
	private final static int ROLE_LENGTH_FIELD = EncUtils.U_INT8;
	private final static int CANDIDATES_LENGTH_FIELD = EncUtils.U_INT16;

	private final ContentType contentType;
	private final byte[] userFragment;
	private final byte[] password;
	private final ApplicationID applicationID;
	private final byte[] role;
	private final List<IceCandidate> candidates;

	private AppAttachReqAns(Builder builder, ContentType contentType) {
		userFragment = builder.userFragment;
		password = builder.password;
		applicationID = builder.applicationID;
		role = builder.role;
		candidates = builder.candidates;
		this.contentType = contentType;
	}

	public AppAttachReqAns(UnsignedByteBuffer buf, ContentType contentType) {
		this.contentType = contentType;
		userFragment = new byte[buf.getLengthValue(UFRAG_LENGTH_FIELD)];
		buf.getRaw(userFragment);

		password = new byte[buf.getLengthValue(PASS_LENGTH_FIELD)];
		buf.getRaw(password);

		int appId = buf.getSigned16();
		applicationID = ApplicationID.valueOf(appId);
		if (applicationID == null)
			throw new DecodingException("Unknown application ID " + appId);

		role = new byte[buf.getLengthValue(ROLE_LENGTH_FIELD)];
		buf.getRaw(role);

		candidates = new ArrayList<IceCandidate>();
		int candLength = buf.getLengthValue(CANDIDATES_LENGTH_FIELD);

		int startPos = buf.position();

		while (buf.getConsumedFrom(startPos) < candLength) {
			IceCandidate candidate = IceCandidate.parse(buf);
			candidates.add(candidate);
		}
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
	public byte[] getRole() {
		return role;
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

	@Override
	protected void implWriteTo(UnsignedByteBuffer buf) {
		Field ufragLenFld = buf.allocateLengthField(UFRAG_LENGTH_FIELD);
		buf.putRaw(userFragment);
		ufragLenFld.setEncodedLength(buf.getConsumedFrom(ufragLenFld.getNextPosition()));
		Field passLenFld = buf.allocateLengthField(PASS_LENGTH_FIELD);
		buf.putRaw(password);
		passLenFld.setEncodedLength(buf.getConsumedFrom(passLenFld.getNextPosition()));
		buf.putUnsigned16(applicationID.getId());
		Field roleLenFld = buf.allocateLengthField(ROLE_LENGTH_FIELD);
		buf.putRaw(role);
		roleLenFld.setEncodedLength(buf.getConsumedFrom(roleLenFld.getNextPosition()));
		writeCandidates(buf);
	}

	private void writeCandidates(UnsignedByteBuffer buf) {
		Field lenFld = buf.allocateLengthField(CANDIDATES_LENGTH_FIELD);

		for (IceCandidate c : candidates) {
			c.writeTo(buf);
		}

		lenFld.setEncodedLength(buf.getConsumedFrom(lenFld.getNextPosition()));
	}

	@Override
	public ContentType getType() {
		return contentType;
	}

	/**
	 * Builder for AppAttach requests and answers
	 * 
	 * @author Daniel Zozin <zdenial@gmx.com>
	 * 
	 */
	public static class Builder {

		private final static byte[] ROLE_ACTIVE = "active".getBytes(Charset.forName("US-ASCII"));
		private final static byte[] ROLE_PASSIVE = "passive".getBytes(Charset.forName("US-ASCII"));

		byte[] userFragment;
		byte[] password;
		ApplicationID applicationID;
		byte[] role;
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
			role = ROLE_PASSIVE;
			return new AppAttachReqAns(this, ContentType.APPATTACH_REQ);
		}

		public AppAttachReqAns buildAnswer() {
			role = ROLE_ACTIVE;
			return new AppAttachReqAns(this, ContentType.APPATTACH_ANS);
		}
	}
}
