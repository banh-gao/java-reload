package com.github.reload.message.content;

import com.github.reload.message.NodeID;

public abstract class LeaveRequest extends MessageContent {

	private static final int DATA_LENGTH_FIELD = EncUtils.U_INT16;

	private final NodeID leavingNode;

	public LeaveRequest(NodeID leavingNode) {
		this.leavingNode = leavingNode;
	}

	public static LeaveRequest parseRequest(Context context, UnsignedByteBuffer buf) {
		NodeID leavingNode = NodeID.valueOf(context.getConfiguration().getNodeIdLength(), buf);

		int len = buf.getLengthValue(DATA_LENGTH_FIELD);
		byte[] data = new byte[len];
		buf.getRaw(data);

		return context.getTopologyPlugin().parseLeaveRequest(leavingNode, UnsignedByteBuffer.wrap(data));
	}

	public NodeID getLeavingNode() {
		return leavingNode;
	}

	protected abstract byte[] getData();

	@Override
	protected final void implWriteTo(UnsignedByteBuffer buf) {
		leavingNode.writeTo(buf);
		byte[] data = getData();
		Field lenFld = buf.allocateLengthField(DATA_LENGTH_FIELD);
		buf.putRaw(data);
		lenFld.setEncodedLength(buf.getConsumedFrom(lenFld.getNextPosition()));
	}

	@Override
	public final ContentType getType() {
		return ContentType.LEAVE_REQ;
	}

}
