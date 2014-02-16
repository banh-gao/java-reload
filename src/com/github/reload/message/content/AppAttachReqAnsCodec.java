package com.github.reload.message.content;

import io.netty.buffer.ByteBuf;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import com.github.reload.Context;
import com.github.reload.net.data.Codec;
import com.github.reload.net.ice.IceCandidate;

public class AppAttachReqAnsCodec extends Codec<AppAttachReqAns> {

	private final static int UFRAG_LENGTH_FIELD = U_INT8;
	private final static int PASS_LENGTH_FIELD = U_INT8;
	private final static int ROLE_LENGTH_FIELD = U_INT8;
	private final static int CANDIDATES_LENGTH_FIELD = U_INT16;

	private final static byte[] ROLE_ACTIVE = "active".getBytes(Charset.forName("US-ASCII"));
	private final static byte[] ROLE_PASSIVE = "passive".getBytes(Charset.forName("US-ASCII"));

	private final Codec<IceCandidate> iceCodec;

	public AppAttachReqAnsCodec(Context context) {
		super(context);
		iceCodec = getCodec(IceCandidate.class);
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

		if (obj.isActive) {
			buf.writeBytes(ROLE_ACTIVE);
		} else {
			buf.writeBytes(ROLE_PASSIVE);
		}

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

		byte[] role = new byte[roleData.readableBytes()];
		buf.readBytes(role);

		obj.isActive = Arrays.equals(role, ROLE_ACTIVE);

		obj.candidates = new ArrayList<IceCandidate>();
		ByteBuf candData = readField(buf, CANDIDATES_LENGTH_FIELD);

		while (candData.readableBytes() > 0) {
			IceCandidate candidate = iceCodec.decode(buf);
			obj.candidates.add(candidate);
		}
		return obj;
	}
}