package com.github.reload.net.encoders.content;

import io.netty.buffer.ByteBuf;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import com.github.reload.conf.Configuration;
import com.github.reload.net.encoders.Codec;
import com.github.reload.net.encoders.Codec.ReloadCodec;
import com.github.reload.net.encoders.content.AppAttachMessage.AppAttachMessageCodec;
import com.github.reload.net.ice.HostCandidate;

@ReloadCodec(AppAttachMessageCodec.class)
public class AppAttachMessage extends Content {

	private byte[] userFragment;
	private byte[] password;
	private int applicationId;
	private boolean isActive;
	private List<HostCandidate> candidates;

	AppAttachMessage() {
	}

	private AppAttachMessage(Builder builder) {
		userFragment = builder.userFragment;
		password = builder.password;
		applicationId = builder.applicationId;
		isActive = builder.isActive;
		candidates = new ArrayList<HostCandidate>(builder.candidates);
	}

	/**
	 * @return The ICE candidiates proposed by the sender
	 */
	public List<HostCandidate> getCandidates() {
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
	public int getApplicationID() {
		return applicationId;
	}

	/**
	 * Builder for AppAttach requests and answers
	 * 
	 */
	public static class Builder {

		byte[] userFragment = new byte[0];
		byte[] password = new byte[0];
		List<? extends HostCandidate> candidates = new ArrayList<HostCandidate>();
		boolean sendUpdate = false;
		boolean isActive;
		int applicationId;

		public Builder(int applicationId) {
			this.applicationId = applicationId;
		}

		public AppAttachMessage buildRequest() {
			isActive = false;
			return new AppAttachMessage(this);
		}

		public AppAttachMessage buildAnswer() {
			isActive = true;
			return new AppAttachMessage(this);
		}

		public Builder candidates(List<? extends HostCandidate> candidates) {
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

	static class AppAttachMessageCodec extends Codec<AppAttachMessage> {

		private final static int UFRAG_LENGTH_FIELD = U_INT8;
		private final static int PASS_LENGTH_FIELD = U_INT8;
		private final static int ROLE_LENGTH_FIELD = U_INT8;
		private final static int CANDIDATES_LENGTH_FIELD = U_INT16;

		private final static byte[] ROLE_ACTIVE = "active".getBytes(Charset.forName("US-ASCII"));
		private final static byte[] ROLE_PASSIVE = "passive".getBytes(Charset.forName("US-ASCII"));

		private final Codec<HostCandidate> iceCodec;

		public AppAttachMessageCodec(Configuration conf) {
			super(conf);
			iceCodec = getCodec(HostCandidate.class);
		}

		@Override
		public void encode(AppAttachMessage obj, ByteBuf buf, Object... params) throws CodecException {
			Field ufragLenFld = allocateField(buf, UFRAG_LENGTH_FIELD);
			buf.writeBytes(obj.userFragment);
			ufragLenFld.updateDataLength();

			Field passLenFld = allocateField(buf, PASS_LENGTH_FIELD);
			buf.writeBytes(obj.password);
			passLenFld.updateDataLength();

			buf.writeShort(obj.applicationId);
			Field roleLenFld = allocateField(buf, ROLE_LENGTH_FIELD);

			if (obj.isActive) {
				buf.writeBytes(ROLE_ACTIVE);
			} else {
				buf.writeBytes(ROLE_PASSIVE);
			}

			roleLenFld.updateDataLength();
			encodeCandidates(obj, buf);
		}

		private void encodeCandidates(AppAttachMessage obj, ByteBuf buf) throws CodecException {
			Field lenFld = allocateField(buf, CANDIDATES_LENGTH_FIELD);

			for (HostCandidate c : obj.candidates) {
				iceCodec.encode(c, buf);
			}

			lenFld.updateDataLength();
		}

		@Override
		public AppAttachMessage decode(ByteBuf buf, Object... params) throws CodecException {
			AppAttachMessage obj = new AppAttachMessage();
			ByteBuf fragFld = readField(buf, UFRAG_LENGTH_FIELD);
			obj.userFragment = new byte[fragFld.readableBytes()];
			fragFld.readBytes(obj.userFragment);
			fragFld.release();

			ByteBuf pswData = readField(buf, PASS_LENGTH_FIELD);
			obj.password = new byte[pswData.readableBytes()];
			pswData.readBytes(obj.password);
			pswData.release();

			obj.applicationId = buf.readShort();

			ByteBuf roleData = readField(buf, ROLE_LENGTH_FIELD);
			byte[] role = new byte[roleData.readableBytes()];
			roleData.readBytes(role);
			roleData.release();

			obj.isActive = Arrays.equals(role, ROLE_ACTIVE);

			obj.candidates = new ArrayList<HostCandidate>();
			ByteBuf candData = readField(buf, CANDIDATES_LENGTH_FIELD);

			while (candData.readableBytes() > 0) {
				HostCandidate candidate = iceCodec.decode(candData);
				obj.candidates.add(candidate);
			}
			candData.release();

			return obj;
		}
	}
}
