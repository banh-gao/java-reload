package com.github.reload.net.codecs.content;

import io.netty.buffer.ByteBuf;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import dagger.ObjectGraph;
import com.github.reload.net.codecs.Codec;
import com.github.reload.net.codecs.Codec.ReloadCodec;
import com.github.reload.net.codecs.content.AttachMessage.AttachMessageCodec;
import com.github.reload.net.ice.HostCandidate;

/**
 * Common representation of attach requests and answers
 * 
 */
@ReloadCodec(AttachMessageCodec.class)
public class AttachMessage extends Content {

	private final byte[] userFragment;
	private final byte[] password;
	private final ContentType type;
	private final List<HostCandidate> candidates;
	private final boolean sendUpdate;

	private AttachMessage(Builder builder) {
		userFragment = builder.userFragment;
		password = builder.password;
		type = builder.contentType;
		candidates = builder.candidates;
		sendUpdate = builder.sendUpdate;
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

	@Override
	public com.github.reload.net.codecs.content.ContentType getType() {
		return type;
	}

	public static class Builder {

		byte[] userFragment = new byte[0];
		byte[] password = new byte[0];
		List<HostCandidate> candidates = new ArrayList<HostCandidate>();
		boolean sendUpdate = false;
		ContentType contentType;

		public Builder() {
		}

		public Builder candidates(List<HostCandidate> candidates) {
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

		public AttachMessage buildRequest() {
			contentType = ContentType.ATTACH_REQ;
			return new AttachMessage(this);
		}

		public AttachMessage buildAnswer() {
			contentType = ContentType.ATTACH_ANS;
			return new AttachMessage(this);
		}
	}

	static class AttachMessageCodec extends Codec<AttachMessage> {

		private final static byte[] ROLE_ACTIVE = "active".getBytes(Charset.forName("US-ASCII"));
		private final static byte[] ROLE_PASSIVE = "passive".getBytes(Charset.forName("US-ASCII"));

		private final static int UFRAG_LENGTH_FIELD = U_INT8;
		private final static int PASS_LENGTH_FIELD = U_INT8;
		private final static int ROLE_LENGTH_FIELD = U_INT8;
		private final static int CANDIDATES_LENGTH_FIELD = U_INT16;

		public AttachMessageCodec(ObjectGraph ctx) {
			super(ctx);
		}

		@Override
		public void encode(AttachMessage obj, ByteBuf buf, Object... params) throws CodecException {
			Field ufragLenFld = allocateField(buf, UFRAG_LENGTH_FIELD);
			buf.writeBytes(obj.userFragment);
			ufragLenFld.updateDataLength();

			Field passLenFld = allocateField(buf, PASS_LENGTH_FIELD);
			buf.writeBytes(obj.password);
			passLenFld.updateDataLength();

			Field roleLenFld = allocateField(buf, ROLE_LENGTH_FIELD);

			if (obj.isRequest()) {
				buf.writeBytes(ROLE_PASSIVE);
			} else {
				buf.writeBytes(ROLE_ACTIVE);
			}

			roleLenFld.updateDataLength();

			encodeCandidates(obj, buf);

			buf.writeByte(obj.sendUpdate ? 1 : 0);
		}

		private void encodeCandidates(AttachMessage obj, ByteBuf buf) throws CodecException {
			Field lenFld = allocateField(buf, CANDIDATES_LENGTH_FIELD);

			for (HostCandidate c : obj.candidates) {
				@SuppressWarnings("unchecked")
				Codec<HostCandidate> iceCodec = (Codec<HostCandidate>) getCodec(c.getClass());
				iceCodec.encode(c, buf);
			}

			lenFld.updateDataLength();
		}

		@Override
		public AttachMessage decode(ByteBuf buf, Object... params) throws CodecException {
			AttachMessage.Builder b = new AttachMessage.Builder();
			ByteBuf usrFrag = readField(buf, UFRAG_LENGTH_FIELD);
			b.userFragment = new byte[usrFrag.readableBytes()];
			usrFrag.readBytes(b.userFragment);

			ByteBuf password = readField(buf, PASS_LENGTH_FIELD);

			b.password = new byte[password.readableBytes()];
			password.readBytes(b.password);

			ByteBuf role = readField(buf, ROLE_LENGTH_FIELD);
			byte[] roleData = new byte[role.readableBytes()];
			role.readBytes(roleData);

			b.candidates = decodeCandidates(buf);

			b.sendUpdate = buf.readByte() > 0;

			if (Arrays.equals(roleData, ROLE_PASSIVE))
				return b.buildRequest();
			else
				return b.buildAnswer();
		}

		private List<HostCandidate> decodeCandidates(ByteBuf buf) throws CodecException {
			List<HostCandidate> cands = new ArrayList<HostCandidate>();
			ByteBuf candidates = readField(buf, CANDIDATES_LENGTH_FIELD);

			while (candidates.readableBytes() > 0) {
				Codec<HostCandidate> codec = getCodec(HostCandidate.class);
				cands.add(codec.decode(candidates));
			}
			return cands;
		}

	}
}