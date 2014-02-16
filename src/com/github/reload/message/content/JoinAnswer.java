package com.github.reload.message.content;


public abstract class JoinAnswer extends MessageContent {

	private static final int DATA_LENGTH_FIELD = EncUtils.U_INT16;

	public static JoinAnswer parseAnswer(TopologyPlugin plugin, UnsignedByteBuffer buf) {
		int len = buf.getLengthValue(DATA_LENGTH_FIELD);
		byte[] data = new byte[len];
		buf.getRaw(data);
		return plugin.parseJoinAnswer(UnsignedByteBuffer.wrap(data));
	}

	@Override
	protected final void implWriteTo(UnsignedByteBuffer buf) {
		byte[] data = getData();
		Field lenFld = buf.allocateLengthField(DATA_LENGTH_FIELD);
		buf.putRaw(data);
		lenFld.setEncodedLength(buf.getConsumedFrom(lenFld.getNextPosition()));
	}

	protected abstract byte[] getData();

	@Override
	public final ContentType getType() {
		return ContentType.JOIN_ANS;
	}

}
