package com.github.reload.message.content;

import com.github.reload.message.NodeID;

public abstract class JoinRequest extends MessageContent {

	private static final int DATA_LENGTH_FIELD = EncUtils.U_INT16;

	private final NodeID joiningNode;

	public static JoinRequest parseRequest(Context context, UnsignedByteBuffer buf) {
		NodeID joiningNode = NodeID.valueOf(context.getConfiguration().getNodeIdLength(), buf);

		int len = buf.getLengthValue(DATA_LENGTH_FIELD);
		byte[] data = new byte[len];
		buf.getRaw(data);
		return context.getTopologyPlugin().parseJoinRequest(joiningNode, UnsignedByteBuffer.wrap(data));
	}

	public JoinRequest(NodeID joiningNode) {
		this.joiningNode = joiningNode;
	}

	public NodeID getJoiningNode() {
		return joiningNode;
	}

	@Override
	protected final void implWriteTo(UnsignedByteBuffer buf) {
		joiningNode.writeTo(buf);

		byte[] data = getData();

		Field lenFld = buf.allocateLengthField(DATA_LENGTH_FIELD);
		buf.putRaw(data);
		lenFld.setEncodedLength(buf.getConsumedFrom(lenFld.getNextPosition()));
	}

	protected abstract byte[] getData();

	@Override
	public ContentType getType() {
		return ContentType.JOIN_REQ;
	}

}
