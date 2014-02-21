package com.github.reload.message.content;

import io.netty.buffer.ByteBuf;
import com.github.reload.Context;
import com.github.reload.message.Content;
import com.github.reload.message.ContentType;
import com.github.reload.message.NodeID;
import com.github.reload.message.content.LeaveRequest.LeaveRequestCodec;
import com.github.reload.net.data.Codec;
import com.github.reload.net.data.ReloadCodec;

@ReloadCodec(LeaveRequestCodec.class)
public class LeaveRequest extends Content {

	private final NodeID leavingNode;
	private final byte[] overlayData;

	public LeaveRequest(NodeID leavingNode, byte[] overlayData) {
		this.leavingNode = leavingNode;
		this.overlayData = overlayData;
	}

	public NodeID getLeavingNode() {
		return leavingNode;
	}

	public byte[] getOverlayData() {
		return overlayData;
	}

	@Override
	public final ContentType getType() {
		return ContentType.LEAVE_REQ;
	}

	public static class LeaveRequestCodec extends Codec<LeaveRequest> {

		private static final int DATA_LENGTH_FIELD = U_INT16;

		private final Codec<NodeID> nodeCodec;

		public LeaveRequestCodec(Context context) {
			super(context);
			nodeCodec = getCodec(NodeID.class);
		}

		@Override
		public void encode(LeaveRequest obj, ByteBuf buf, Object... params) throws com.github.reload.net.data.Codec.CodecException {
			nodeCodec.encode(obj.leavingNode, buf);

			Field lenFld = allocateField(buf, DATA_LENGTH_FIELD);
			buf.writeBytes(obj.overlayData);
			lenFld.updateDataLength();
		}

		@Override
		public LeaveRequest decode(ByteBuf buf, Object... params) throws com.github.reload.net.data.Codec.CodecException {
			NodeID leavingNode = nodeCodec.decode(buf);

			ByteBuf overlayData = readField(buf, DATA_LENGTH_FIELD);

			byte[] data = new byte[overlayData.readableBytes()];
			overlayData.readBytes(data);

			overlayData.release();

			return new LeaveRequest(leavingNode, data);
		}

	}

}
