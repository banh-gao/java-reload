package com.github.reload.message.content;

import io.netty.buffer.ByteBuf;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import com.github.reload.Context;
import com.github.reload.message.Content;
import com.github.reload.message.ContentType;
import com.github.reload.message.content.AttachMessage.AttachMessageCodec;
import com.github.reload.net.data.Codec;
import com.github.reload.net.data.ReloadCodec;
import com.github.reload.net.ice.IceCandidate;

/**
 * Common representation of attach requests and answers
 * 
 * @author Daniel Zozin <zdenial@gmx.com>
 * 
 */
@ReloadCodec(AttachMessageCodec.class)
public class AttachMessage extends Content {

	private byte[] userFragment;
	private byte[] password;
	private ContentType type;
	private List<IceCandidate> candidates;
	private boolean sendUpdate;

	private AttachMessage(Builder builder, ContentType type) {
		userFragment = builder.userFragment;
		password = builder.password;
		this.type = type;
		candidates = builder.candidates;
		sendUpdate = builder.sendUpdate;
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
	public com.github.reload.message.ContentType getType() {
		return type;
	}

	public static class Builder {

		byte[] userFragment = new byte[0];
		byte[] password = new byte[0];
		List<IceCandidate> candidates = new ArrayList<IceCandidate>();
		boolean sendUpdate = false;

		public Builder() {
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

		public AttachMessage buildRequest() {
			return new AttachMessage(this, ContentType.ATTACH_REQ);
		}

		public AttachMessage buildAnswer() {
			return new AttachMessage(this, ContentType.ATTACH_ANS);
		}
	}

	public static class AttachMessageCodec extends Codec<AttachMessage> {

		private final static byte[] ROLE_ACTIVE = "active".getBytes(Charset.forName("US-ASCII"));
		private final static byte[] ROLE_PASSIVE = "passive".getBytes(Charset.forName("US-ASCII"));

		private final static int UFRAG_LENGTH_FIELD = U_INT8;
		private final static int PASS_LENGTH_FIELD = U_INT8;
		private final static int ROLE_LENGTH_FIELD = U_INT8;
		private final static int CANDIDATES_LENGTH_FIELD = U_INT16;

		public AttachMessageCodec(Context context) {
			super(context);
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

			if (obj.isRequest())
				buf.writeBytes(ROLE_PASSIVE);
			else
				buf.writeBytes(ROLE_ACTIVE);

			roleLenFld.updateDataLength();

			encodeCandidates(obj, buf);

			buf.writeByte(obj.sendUpdate ? 1 : 0);
		}

		private void encodeCandidates(AttachMessage obj, ByteBuf buf) throws com.github.reload.net.data.Codec.CodecException {
			Field lenFld = allocateField(buf, CANDIDATES_LENGTH_FIELD);

			for (IceCandidate c : obj.candidates) {
				@SuppressWarnings("unchecked")
				Codec<IceCandidate> iceCodec = (Codec<IceCandidate>) getCodec(c.getClass());
				iceCodec.encode(c, buf);
			}

			lenFld.updateDataLength();
		}

		@Override
		public AttachMessage decode(ByteBuf buf, Object... params) throws CodecException {
			AttachMessage.Builder b = new AttachMessage.Builder();
			ByteBuf usrFrag = readField(buf, UFRAG_LENGTH_FIELD);
			b.userFragment = new byte[usrFrag.readableBytes()];
			buf.readBytes(b.userFragment);

			ByteBuf password = readField(buf, PASS_LENGTH_FIELD);

			b.password = new byte[password.readableBytes()];
			buf.readBytes(b.password);

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

		private List<IceCandidate> decodeCandidates(ByteBuf buf) throws com.github.reload.net.data.Codec.CodecException {
			List<IceCandidate> cands = new ArrayList<IceCandidate>();
			ByteBuf candidates = readField(buf, CANDIDATES_LENGTH_FIELD);

			while (candidates.readableBytes() > 0) {
				Codec<IceCandidate> codec = getCodec(IceCandidate.class);
				cands.add(codec.decode(buf));
			}
			return cands;
		}

	}
}