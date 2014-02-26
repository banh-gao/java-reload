package com.github.reload.net.data;

import io.netty.buffer.ByteBuf;

/**
 * RELOAD link layer message
 */
public abstract class FramedMessage {

	public static enum FrameType {
		DATA(128), ACK(129);

		public final int code;

		FrameType(int code) {
			this.code = code;
		}

		public static FrameType valueOf(int code) {
			for (FrameType t : FrameType.values())
				if (t.code == code)
					return t;
			return null;
		}
	};

	// Maximum sequence number
	public static final long SEQ_MAX_VALUE = 0xffffffff;

	public abstract long getSequence();

	public abstract FrameType getType();

	/**
	 * Link level data message
	 */
	public static class FramedData extends FramedMessage {

		protected long sequence;
		protected ByteBuf payload;

		public FramedData(long sequence, ByteBuf payload) {
			this.sequence = sequence;
			this.payload = payload;
		}

		@Override
		public FrameType getType() {
			return FrameType.DATA;
		}

		@Override
		public long getSequence() {
			return sequence;
		}

		public ByteBuf getPayload() {
			return payload;
		}
	}

	/**
	 * Link level acknowledge message
	 */
	public static class FramedAck extends FramedMessage {

		protected final long ack_sequence;
		protected final int receivedBitMask;

		public FramedAck(long ack_sequence, int receivedBitMask) {
			super();
			this.ack_sequence = ack_sequence;
			this.receivedBitMask = receivedBitMask;
		}

		@Override
		public FrameType getType() {
			return FrameType.ACK;
		}

		@Override
		public long getSequence() {
			return ack_sequence;
		}

		public int getReceivedMask() {
			return receivedBitMask;
		}
	}
}
