package com.github.reload.message.content;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import net.sf.jReload.message.ContentType;
import net.sf.jReload.message.EncUtils;
import net.sf.jReload.message.MessageContent;
import net.sf.jReload.message.UnsignedByteBuffer;
import net.sf.jReload.message.UnsignedByteBuffer.Field;
import net.sf.jReload.net.ice.ICEHelper;
import net.sf.jReload.net.ice.IceCandidate;

/**
 * Common representation of attach requests and answers
 * 
 * @author Daniel Zozin <zdenial@gmx.com>
 * 
 */
public class AttachReqAns extends MessageContent {

	private final static int UFRAG_LENGTH_FIELD = EncUtils.U_INT8;
	private final static int PASS_LENGTH_FIELD = EncUtils.U_INT8;
	private final static int ROLE_LENGTH_FIELD = EncUtils.U_INT8;
	private final static int CANDIDATES_LENGTH_FIELD = EncUtils.U_INT16;

	private final ContentType contentType;

	private final byte[] userFragment;
	private final byte[] password;
	private final byte[] role;
	private final List<IceCandidate> candidates;
	private final boolean sendUpdate;

	private AttachReqAns(Builder builder, ContentType contentType) {
		this.contentType = contentType;
		userFragment = builder.userFragment;
		password = builder.password;
		role = builder.role;
		candidates = builder.candidates;
		sendUpdate = builder.sendUpdate;
	}

	public AttachReqAns(UnsignedByteBuffer buf, ContentType contentType) {
		this.contentType = contentType;

		userFragment = new byte[buf.getLengthValue(UFRAG_LENGTH_FIELD)];
		buf.getRaw(userFragment);

		password = new byte[buf.getLengthValue(PASS_LENGTH_FIELD)];
		buf.getRaw(password);

		role = new byte[buf.getLengthValue(ROLE_LENGTH_FIELD)];
		buf.getRaw(role);

		candidates = new ArrayList<IceCandidate>();

		int candLength = buf.getLengthValue(CANDIDATES_LENGTH_FIELD);

		int startPos = buf.position();

		while (buf.getConsumedFrom(startPos) < candLength) {
			IceCandidate candidate = IceCandidate.parse(buf);
			candidates.add(candidate);
		}

		sendUpdate = buf.getRaw8() > 0;
	}

	@Override
	protected void implWriteTo(UnsignedByteBuffer buf) {
		Field ufragLenFld = buf.allocateLengthField(UFRAG_LENGTH_FIELD);
		buf.putRaw(userFragment);
		ufragLenFld.setEncodedLength(buf.getConsumedFrom(ufragLenFld.getNextPosition()));
		Field passLenFld = buf.allocateLengthField(PASS_LENGTH_FIELD);
		buf.putRaw(password);
		passLenFld.setEncodedLength(buf.getConsumedFrom(passLenFld.getNextPosition()));
		Field roleLenFld = buf.allocateLengthField(ROLE_LENGTH_FIELD);
		buf.putRaw(role);
		roleLenFld.setEncodedLength(buf.getConsumedFrom(roleLenFld.getNextPosition()));

		writeCandidates(buf);
		buf.putRaw8((byte) (sendUpdate ? 1 : 0));
	}

	private void writeCandidates(UnsignedByteBuffer buf) {
		Field lenFld = buf.allocateLengthField(CANDIDATES_LENGTH_FIELD);

		for (IceCandidate c : candidates) {
			c.writeTo(buf);
		}

		lenFld.setEncodedLength(buf.getConsumedFrom(lenFld.getNextPosition()));
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
	 * @return true if
	 */
	public boolean isSendUpdate() {
		return sendUpdate;
	}

	public static class Builder {

		private final static byte[] ROLE_ACTIVE = "active".getBytes(Charset.forName("US-ASCII"));
		private final static byte[] ROLE_PASSIVE = "passive".getBytes(Charset.forName("US-ASCII"));

		byte[] userFragment;
		byte[] password;
		byte[] role;
		List<IceCandidate> candidates;
		boolean sendUpdate;

		public Builder(InetSocketAddress listeningAddress, ICEHelper iceHelper) {
			userFragment = iceHelper.getUserFragment();
			password = iceHelper.getPassword();
			candidates = iceHelper.getCandidates(listeningAddress);
		}

		public Builder sendUpdate(boolean sendUpdate) {
			this.sendUpdate = sendUpdate;
			return this;
		}

		public AttachReqAns buildRequest() {
			role = ROLE_PASSIVE;
			return new AttachReqAns(this, ContentType.ATTACH_REQ);
		}

		public AttachReqAns buildAnswer() {
			role = ROLE_ACTIVE;
			return new AttachReqAns(this, ContentType.ATTACH_ANS);
		}
	}

	@Override
	public ContentType getType() {
		return contentType;
	}
}