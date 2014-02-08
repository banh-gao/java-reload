package com.github.reload.message.content;

import io.netty.buffer.ByteBuf;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import com.github.reload.Context;
import com.github.reload.message.Content;
import com.github.reload.net.data.Codec;
import com.github.reload.net.ice.ICEHelper;
import com.github.reload.net.ice.IceCandidate;

/**
 * common representation of AppAttach requests and answers
 * 
 * @author Daniel Zozin <zdenial@gmx.com>
 * 
 */
public class AppAttachReqAns extends Content {

	private byte[] userFragment;
	private byte[] password;
	private ApplicationID applicationID;
	private byte[] role;
	private List<IceCandidate> candidates;

	private AppAttachReqAns() {
	}

	private AppAttachReqAns(Builder builder) {
		userFragment = builder.userFragment;
		password = builder.password;
		applicationID = builder.applicationID;
		role = builder.role;
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

	public static class AppAttachReqAnsCodec extends Codec<AppAttachReqAns> {

		private final static int UFRAG_LENGTH_FIELD = U_INT8;
		private final static int PASS_LENGTH_FIELD = U_INT8;
		private final static int ROLE_LENGTH_FIELD = U_INT8;
		private final static int CANDIDATES_LENGTH_FIELD = U_INT16;

		private final Codec<IceCandidate> iceCodec;

		public AppAttachReqAnsCodec(Context context) {
			super(context);
			iceCodec = getCodec(IceCandidate.class, context);
		}

		@Override
		public void encode(AppAttachReqAns obj, ByteBuf buf) throws CodecException {
			Field ufragLenFld = allocateField(buf, UFRAG_LENGTH_FIELD);
			buf.writeBytes(obj.userFragment);
			ufragLenFld.updateDataLength();

			Field passLenFld = allocateField(buf, PASS_LENGTH_FIELD);
			buf.writeBytes(obj.password);
			passLenFld.updateDataLength();

			buf.writeShort(obj.applicationID.getId());
			Field roleLenFld = allocateField(buf, ROLE_LENGTH_FIELD);
			buf.writeBytes(obj.role);
			roleLenFld.updateDataLength();
			encodeCandidates(obj, buf);
		}

		private void encodeCandidates(AppAttachReqAns obj, ByteBuf buf) throws CodecException {
			Field lenFld = allocateField(buf, CANDIDATES_LENGTH_FIELD);

			for (IceCandidate c : obj.candidates) {
				iceCodec.encode(c, buf);
			}

			lenFld.updateDataLength();
		}

		@Override
		public AppAttachReqAns decode(ByteBuf buf) throws CodecException {
			AppAttachReqAns obj = new AppAttachReqAns();
			ByteBuf fragFld = readField(buf, UFRAG_LENGTH_FIELD);
			obj.userFragment = new byte[fragFld.readableBytes()];
			fragFld.readBytes(obj.userFragment);

			ByteBuf pswData = readField(buf, PASS_LENGTH_FIELD);
			obj.password = new byte[pswData.readableBytes()];
			pswData.readBytes(obj.password);

			int appId = buf.readShort();
			obj.applicationID = ApplicationID.valueOf(appId);
			if (obj.applicationID == null)
				throw new CodecException("Unknown application ID " + appId);

			ByteBuf roleData = readField(buf, ROLE_LENGTH_FIELD);

			obj.role = new byte[roleData.readableBytes()];
			buf.readBytes(obj.role);

			obj.candidates = new ArrayList<IceCandidate>();
			ByteBuf candData = readField(buf, CANDIDATES_LENGTH_FIELD);

			while (candData.readableBytes() > 0) {
				IceCandidate candidate = iceCodec.decode(buf);
				obj.candidates.add(candidate);
			}
			return obj;
		}

	}
}
