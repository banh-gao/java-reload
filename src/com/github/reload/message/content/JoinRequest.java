package com.github.reload.message.content;

import io.netty.buffer.ByteBuf;
import com.github.reload.Context;
import com.github.reload.message.Content;
import com.github.reload.message.ContentType;
import com.github.reload.message.NodeID;
import com.github.reload.message.content.JoinRequest.JoinRequestCodec;
import com.github.reload.net.data.Codec;
import com.github.reload.net.data.ReloadCodec;

@ReloadCodec(JoinRequestCodec.class)
public class JoinRequest extends Content {

	private final NodeID joiningNode;
	private final byte[] overlayData;

	public JoinRequest(NodeID joiningNode, byte[] overlayData) {
		this.joiningNode = joiningNode;
		this.overlayData = overlayData;
	}

	public NodeID getJoiningNode() {
		return joiningNode;
	}

	public byte[] getOverlayData() {
		return overlayData;
	}

	@Override
	public ContentType getType() {
		return ContentType.JOIN_REQ;
	}

	public static class JoinRequestCodec extends Codec<JoinRequest> {

		private static final int DATA_LENGTH_FIELD = U_INT16;

		private final Codec<NodeID> nodeCodec;

		public JoinRequestCodec(Context context) {
			super(context);
			nodeCodec = getCodec(NodeID.class);
		}

		@Override
		public void encode(JoinRequest obj, ByteBuf buf, Object... params) throws com.github.reload.net.data.Codec.CodecException {
			nodeCodec.encode(obj.joiningNode, buf);

			Field lenFld = allocateField(buf, DATA_LENGTH_FIELD);
			buf.writeBytes(obj.overlayData);
			lenFld.updateDataLength();
		}

		@Override
		public JoinRequest decode(ByteBuf buf, Object... params) throws com.github.reload.net.data.Codec.CodecException {
			NodeID joiningNode = nodeCodec.decode(buf);

			ByteBuf overlayData = readField(buf, DATA_LENGTH_FIELD);
			byte[] data = new byte[overlayData.readableBytes()];
			buf.readBytes(data);
			buf.release();

			return new JoinRequest(joiningNode, data);
		}

	}

}
